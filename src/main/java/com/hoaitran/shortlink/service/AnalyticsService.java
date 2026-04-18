package com.hoaitran.shortlink.service;

import com.hoaitran.shortlink.entity.ClickLog;
import com.hoaitran.shortlink.entity.UrlLink;
import com.hoaitran.shortlink.repository.ClickLogRepository;
import com.hoaitran.shortlink.repository.UrlLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    private final ClickLogRepository clickLogRepository;
    private final UrlLinkRepository urlLinkRepository;

    @Async("analyticsTaskExecutor")
    @Transactional
    public void recordClick(String shortCode, String ipAddress, String userAgent, String referer) {
        log.info("Recording click for code: {} from IP: {}", shortCode, ipAddress);
        
        urlLinkRepository.findByShortCode(shortCode).ifPresent(urlLink -> {
            // 1. Increment total click count on the link
            urlLink.setClickCount(urlLink.getClickCount() + 1);
            urlLinkRepository.save(urlLink);

            // 2. Create detailed click log
            ClickLog clickLog = ClickLog.builder()
                    .urlLink(urlLink)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .referer(referer)
                    .build();
            clickLogRepository.save(clickLog);
        });
    }
}
