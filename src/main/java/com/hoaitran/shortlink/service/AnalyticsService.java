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

import com.hoaitran.shortlink.dto.response.ClickLogDTO;
import com.hoaitran.shortlink.dto.response.LinkStatsResponse;
import com.hoaitran.shortlink.dto.response.UrlResponseDTO;
import com.hoaitran.shortlink.entity.User;
import com.hoaitran.shortlink.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    private final ClickLogRepository clickLogRepository;
    private final UrlLinkRepository urlLinkRepository;
    private final UrlShortenerService urlShortenerService;

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
    public LinkStatsResponse getLinkStats(String shortCode, User currentUser, String baseUrl) {
        UrlLink urlLink = urlLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));
        
        // Ownership check
        if (urlLink.getUser() == null || !urlLink.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view stats for this link");
        }

        List<ClickLog> clicks = clickLogRepository.findByUrlLinkShortCode(shortCode);
        
        return LinkStatsResponse.builder()
                .link(urlShortenerService.mapToUrlDTO(urlLink, baseUrl))
                .recentClicks(clicks.stream()
                        .map(this::mapToClickLogDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<UrlResponseDTO> getTopLinks(String baseUrl) {
        return urlLinkRepository.findTop10ByOrderByClickCountDesc().stream()
                .map(link -> urlShortenerService.mapToUrlDTO(link, baseUrl))
                .collect(Collectors.toList());
    }


    private ClickLogDTO mapToClickLogDTO(ClickLog clickLog) {
        return ClickLogDTO.builder()
                .ipAddress(maskIpAddress(clickLog.getIpAddress()))
                .userAgent(clickLog.getUserAgent())
                .referer(clickLog.getReferer())
                .clickedAt(clickLog.getClickedAt())
                .build();
    }

    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return ipAddress;
        }
        if (ipAddress.contains(":")) {
            int lastSeparator = ipAddress.lastIndexOf(':');
            return lastSeparator > 0 ? ipAddress.substring(0, lastSeparator) + ":*" : "*";
        }
        int lastDot = ipAddress.lastIndexOf('.');
        return lastDot > 0 ? ipAddress.substring(0, lastDot) + ".*" : "*";
    }
}
