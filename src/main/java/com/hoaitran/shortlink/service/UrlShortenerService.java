package com.hoaitran.shortlink.service;

import com.hoaitran.shortlink.entity.UrlLink;
import com.hoaitran.shortlink.exception.ResourceNotFoundException;
import com.hoaitran.shortlink.repository.UrlLinkRepository;
import com.hoaitran.shortlink.util.Base62Utils;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.exception.AliasAlreadyExistsException;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {
    private final UrlLinkRepository urlLinkRepository;
    private static final int CODE_LENGTH = 7;

    @Transactional
    public UrlLink shortenUrl(ShortenRequest request) {
        String originalUrl = request.getOriginalUrl();
        String customAlias = request.getCustomAlias();

        if (customAlias != null && !customAlias.isBlank()) {
            if (urlLinkRepository.existsByShortCode(customAlias)) {
                throw new AliasAlreadyExistsException("Alias already in use: " + customAlias);
            }
            UrlLink urlLink = UrlLink.builder()
                    .originalUrl(originalUrl)
                    .shortCode(customAlias)
                    .expiresAt(request.getExpiresAt())
                    .build();
            return urlLinkRepository.save(urlLink);
        }

        // De-duplication: check if URL already exists without custom alias and without expiration (simple version)
        // If user provides expiration, we usually want a new link or update the old one? 
        // User said "expiration theo request", usually implies this specific link.
        
        return urlLinkRepository.findByOriginalUrl(originalUrl)
                .filter(url -> url.getExpiresAt() == null || url.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(url -> request.getExpiresAt() == null || (url.getExpiresAt() != null && url.getExpiresAt().equals(request.getExpiresAt())))
                .orElseGet(() -> {
                    String code;
                    do {
                        code = Base62Utils.generateRandomCode(CODE_LENGTH);
                    } while (urlLinkRepository.existsByShortCode(code));

                    UrlLink urlLink = UrlLink.builder()
                            .originalUrl(originalUrl)
                            .shortCode(code)
                            .expiresAt(request.getExpiresAt())
                            .build();

                    return urlLinkRepository.save(urlLink);
                });
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "urls", key = "#shortCode")
    public String getOriginalUrl(String shortCode) {
        return urlLinkRepository.findByShortCode(shortCode)
                .filter(url -> url.isActive() && (url.getExpiresAt() == null || url.getExpiresAt().isAfter(LocalDateTime.now())))
                .map(UrlLink::getOriginalUrl)
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
