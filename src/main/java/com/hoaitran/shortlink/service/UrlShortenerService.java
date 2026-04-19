package com.hoaitran.shortlink.service;

import com.hoaitran.shortlink.entity.UrlLink;
import com.hoaitran.shortlink.entity.User;
import com.hoaitran.shortlink.exception.ResourceNotFoundException;
import com.hoaitran.shortlink.repository.UrlLinkRepository;
import com.hoaitran.shortlink.util.Base62Utils;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.exception.AliasAlreadyExistsException;
import com.hoaitran.shortlink.exception.IdempotencyConflictException;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {
    private final UrlLinkRepository urlLinkRepository;
    private final MeterRegistry meterRegistry;
    private static final int CODE_LENGTH = 7;

    @Transactional
    public UrlLink shortenUrl(ShortenRequest request, String idempotencyKey, User user) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return urlLinkRepository.findByIdempotencyKey(idempotencyKey)
                    .map(existingLink -> {
                        if (matchesRequest(existingLink, request)) {
                            return existingLink;
                        }
                        throw new IdempotencyConflictException(
                                "Idempotency-Key was already used for a different shorten request");
                    })
                    .orElseGet(() -> createShortUrl(request, idempotencyKey, user));
        }

        return createShortUrl(request, null, user);
    }

    private UrlLink createShortUrl(ShortenRequest request, String idempotencyKey, User user) {
        String originalUrl = request.getOriginalUrl();
        String customAlias = request.getCustomAlias();
        LocalDateTime normalizedExpiresAt = normalizeExpiresAt(request.getExpiresAt());

        if (customAlias != null && !customAlias.isBlank()) {
            return urlLinkRepository.findByShortCode(customAlias)
                    .map(existingLink -> {
                        if (existingLink.getOriginalUrl().equals(originalUrl)) {
                            return existingLink; // Idempotency
                        } else {
                            throw new AliasAlreadyExistsException("Alias already in use: " + customAlias);
                        }
                    })
                    .orElseGet(() -> {
                        UrlLink urlLink = UrlLink.builder()
                                .originalUrl(originalUrl)
                                .shortCode(customAlias)
                                .idempotencyKey(idempotencyKey)
                                .requestedCustomAlias(customAlias)
                                .expiresAt(normalizedExpiresAt)
                                .user(user)
                                .build();
                        return urlLinkRepository.save(urlLink);
                    });
        }

        // Simplistic De-duplication check: Since findByOriginalUrl returns Optional,
        // it may throw NonUniqueResultException if multiple users shorten the same url.
        // We catch exception or just find first if we changed repository.
        // For now, keeping existing logic, just adding .user(user) to new links.
        return urlLinkRepository.findByOriginalUrl(originalUrl)
                // Optionally filter by user to only return if same user shortened it before
                .filter(url -> url.getUser() != null && url.getUser().getId().equals(user.getId()))
                .filter(url -> url.getExpiresAt() == null || url.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(url -> normalizedExpiresAt == null
                        || (url.getExpiresAt() != null && url.getExpiresAt().equals(normalizedExpiresAt)))
                .orElseGet(() -> {
                    String code;
                    do {
                        code = Base62Utils.generateRandomCode(CODE_LENGTH);
                    } while (urlLinkRepository.existsByShortCode(code));

                    UrlLink urlLink = UrlLink.builder()
                            .originalUrl(originalUrl)
                            .shortCode(code)
                            .idempotencyKey(idempotencyKey)
                            .requestedCustomAlias(customAlias)
                            .expiresAt(normalizedExpiresAt)
                            .user(user)
                            .build();

                    UrlLink saved = urlLinkRepository.save(urlLink);
                    meterRegistry.counter("shortlink.created").increment();
                    return saved;
                });
    }

    private boolean matchesRequest(UrlLink existingLink, ShortenRequest request) {
        boolean sameOriginalUrl = existingLink.getOriginalUrl().equals(request.getOriginalUrl());
        boolean sameAlias = normalizeAlias(existingLink.getRequestedCustomAlias()).equals(normalizeAlias(request.getCustomAlias()));
        boolean sameExpiry = java.util.Objects.equals(existingLink.getExpiresAt(), normalizeExpiresAt(request.getExpiresAt()));
        return sameOriginalUrl && sameAlias && sameExpiry;
    }

    private String normalizeAlias(String alias) {
        return alias == null ? "" : alias.trim();
    }

    private LocalDateTime normalizeExpiresAt(LocalDateTime expiresAt) {
        return expiresAt == null ? null : expiresAt.truncatedTo(ChronoUnit.MICROS);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "urls", key = "#shortCode")
    public String getOriginalUrl(String shortCode) {
        return urlLinkRepository.findByShortCode(shortCode)
                .filter(url -> url.isActive() && (url.getExpiresAt() == null || url.getExpiresAt().isAfter(LocalDateTime.now())))
                .map(url -> {
                    meterRegistry.counter("shortlink.redirect").increment();
                    return url.getOriginalUrl();
                })
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found, inactive, or expired: " + shortCode));
    }

    @Transactional
    public void deleteUrl(String shortCode, User currentUser) {
        UrlLink urlLink = urlLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));
        
        checkOwnership(urlLink, currentUser);
        
        urlLinkRepository.delete(urlLink);
    }

    @Transactional
    public UrlLink updateStatus(String shortCode, boolean active, User currentUser) {
        UrlLink urlLink = urlLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));
        
        checkOwnership(urlLink, currentUser);
        
        urlLink.setActive(active);
        return urlLinkRepository.save(urlLink);
    }
    
    // Add check ownership method for stats if we want to restrict
    public void checkOwnership(UrlLink urlLink, User currentUser) {
        if (urlLink.getUser() == null || !urlLink.getUser().getId().equals(currentUser.getId())) {
             throw new AccessDeniedException("You do not have permission to modify this link");
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<UrlLink> getUserLinks(User user) {
        return urlLinkRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
    }
}
