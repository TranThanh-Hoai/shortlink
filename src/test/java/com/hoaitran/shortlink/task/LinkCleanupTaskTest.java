package com.hoaitran.shortlink.task;

import com.hoaitran.shortlink.repository.ClickEventRepository;
import com.hoaitran.shortlink.repository.LinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LinkCleanupTaskTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    @InjectMocks
    private LinkCleanupTask linkCleanupTask;

    @Test
    void cleanupExpiredLinks_ShouldCallRepositoryDelete() {
        linkCleanupTask.cleanupExpiredLinks();
        verify(clickEventRepository).deleteByLinkExpiresAtBefore(any(LocalDateTime.class));
        verify(linkRepository).deleteExpiredLinks(any(LocalDateTime.class));
    }
}
