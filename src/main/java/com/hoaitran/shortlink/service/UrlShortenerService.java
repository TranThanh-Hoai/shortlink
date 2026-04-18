package com.hoaitran.shortlink.service;

import com.hoaitran.shortlink.entity.UrlLink;
import com.hoaitran.shortlink.exception.ResourceNotFoundException;
import com.hoaitran.shortlink.repository.UrlLinkRepository;
import com.hoaitran.shortlink.util.Base62Utils;
import lombok.RequiredArgsConstructor;
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
    public String getOriginalUrl(String shortCode) {
        return urlLinkRepository.findByShortCode(shortCode)
                .filter(UrlLink::isActive)
                .map(UrlLink::getOriginalUrl)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found or inactive: " + shortCode));
    }
}
