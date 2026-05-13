package com.hoaitran.shortlink.task;

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

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void cleanupExpiredLinks() {
        log.info("Running expired links cleanup task...");
        try {
            linkRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());
            log.info("Successfully cleaned up expired links.");
        } catch (Exception e) {
            log.error("Error occurred while cleaning up expired links", e);
        }
    }
}
