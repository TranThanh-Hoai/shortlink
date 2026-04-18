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

import com.hoaitran.shortlink.dto.response.LinkStatsResponse;
import com.hoaitran.shortlink.exception.ResourceNotFoundException;
import java.util.List;

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
            // 1. Increment total click count on the link (Atomic update)
            urlLinkRepository.incrementClickCount(shortCode);

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

    @Transactional(readOnly = true)
    public LinkStatsResponse getLinkStats(String shortCode) {
        UrlLink urlLink = urlLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));
        
        List<ClickLog> clicks = clickLogRepository.findByUrlLinkShortCode(shortCode);
        
        return LinkStatsResponse.builder()
                .link(urlLink)
                .recentClicks(clicks)
                .build();
    }

    @Transactional(readOnly = true)
    public List<UrlLink> getTopLinks() {
        return urlLinkRepository.findTop10ByOrderByClickCountDesc();
    }
}
