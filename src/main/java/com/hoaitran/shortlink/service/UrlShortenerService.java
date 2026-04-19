package com.hoaitran.shortlink.service;

import com.hoaitran.shortlink.entity.UrlLink;
import com.hoaitran.shortlink.exception.ResourceNotFoundException;
import com.hoaitran.shortlink.repository.UrlLinkRepository;
import com.hoaitran.shortlink.util.Base62Utils;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.cache.annotation.Cacheable;
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
    public UrlLink shortenUrl(ShortenRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return urlLinkRepository.findByIdempotencyKey(idempotencyKey)
                    .map(existingLink -> {
                        if (matchesRequest(existingLink, request)) {
                            return existingLink;
                        }
                        throw new IdempotencyConflictException(
                                "Idempotency-Key was already used for a different shorten request");
                    })
                    .orElseGet(() -> createShortUrl(request, idempotencyKey));
        }

        return createShortUrl(request, null);
    }

    private UrlLink createShortUrl(ShortenRequest request, String idempotencyKey) {
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
                                .build();
                        return urlLinkRepository.save(urlLink);
                    });
        }

        // De-duplication: check if URL already exists without custom alias and without expiration (simple version)
        // If user provides expiration, we usually want a new link or update the old one? 
        // User said "expiration theo request", usually implies this specific link.
        
        return urlLinkRepository.findByOriginalUrl(originalUrl)
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
    public void deleteUrl(String shortCode) {
        UrlLink urlLink = urlLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));
        urlLinkRepository.delete(urlLink);
    }

    @Transactional
    public UrlLink updateStatus(String shortCode, boolean active) {
        UrlLink urlLink = urlLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));
        urlLink.setActive(active);
        return urlLinkRepository.save(urlLink);
    }
}
