package com.hoaitran.shortlink.task;

import com.hoaitran.shortlink.repository.ClickEventRepository;
import com.hoaitran.shortlink.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class LinkCleanupTask {

    private final LinkRepository linkRepository;
    private final ClickEventRepository clickEventRepository;

    /**
     * Cleans up expired links every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredLinks() {
        log.info("Starting cleanup of expired links...");
        try {
            LocalDateTime now = LocalDateTime.now();
            clickEventRepository.deleteByLinkExpiresAtBefore(now);
            linkRepository.deleteExpiredLinks(now);
            log.info("Cleanup of expired links completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during expired links cleanup: ", e);
        }
    }
}
