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

@Service
@RequiredArgsConstructor
public class UrlShortenerService {
    private final UrlLinkRepository urlLinkRepository;
    private static final int CODE_LENGTH = 7;

    @Transactional
    public UrlLink shortenUrl(String originalUrl) {
        String code;
        // Simple collision handling: generate until unique
        do {
            code = Base62Utils.generateRandomCode(CODE_LENGTH);
        } while (urlLinkRepository.existsByShortCode(code));

        UrlLink urlLink = UrlLink.builder()
                .originalUrl(originalUrl)
                .shortCode(code)
                .build();

        return urlLinkRepository.save(urlLink);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "urls", key = "#shortCode")
    public String getOriginalUrl(String shortCode) {
        return urlLinkRepository.findByShortCode(shortCode)
                .filter(url -> url.isActive() && (url.getExpiresAt() == null || url.getExpiresAt().isAfter(LocalDateTime.now())))
                .map(UrlLink::getOriginalUrl)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found, inactive, or expired: " + shortCode));
    }
}
