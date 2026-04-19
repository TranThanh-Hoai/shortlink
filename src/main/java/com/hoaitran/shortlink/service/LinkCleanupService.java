package com.hoaitran.shortlink.service;

import com.hoaitran.shortlink.entity.UrlLink;
import com.hoaitran.shortlink.repository.UrlLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkCleanupService {
    private final UrlLinkRepository urlLinkRepository;
    private final CacheManager cacheManager;

    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional(readOnly = true)
    public void evictExpiredLinks() {
        log.info("Starting scheduled cleanup of expired links from cache");
        List<UrlLink> expiredLinks = urlLinkRepository.findByExpiresAtBefore(LocalDateTime.now());
        
        if (expiredLinks.isEmpty()) {
            return;
        }

        var cache = cacheManager.getCache("urls");
        if (cache != null) {
            for (UrlLink link : expiredLinks) {
                cache.evict(link.getShortCode());
                log.debug("Evicted expired link from cache: {}", link.getShortCode());
            }
        }
        log.info("Evicted {} expired links from cache", expiredLinks.size());
    }
}
