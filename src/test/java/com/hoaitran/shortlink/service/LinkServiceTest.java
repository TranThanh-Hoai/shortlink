package com.hoaitran.shortlink.service;

import com.hoaitran.shortlink.entity.Link;
import com.hoaitran.shortlink.exception.LinkExpiredException;
import com.hoaitran.shortlink.mapper.LinkMapper;
import com.hoaitran.shortlink.repository.LinkRepository;
import com.hoaitran.shortlink.utils.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ClickEventService clickEventService;

    @Mock
    private UserService userService;

    @Mock
    private LinkMapper linkMapper;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @InjectMocks
    private LinkService linkService;

    @BeforeEach
    void setUp() {
        // Mock redis value operations
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getOriginalUrl_ShouldThrowException_WhenLinkIsExpired() {
        // Given
        String shortCode = "expiredCode";
        Link expiredLink = Link.builder()
                .shortCode(shortCode)
                .originalUrl("https://google.com")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .isActive(true)
                .build();

        when(valueOperations.get(anyString())).thenReturn(null);
        when(linkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(expiredLink));

        // When & Then
        assertThrows(LinkExpiredException.class, () -> linkService.getOriginalUrl(shortCode));
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void getOriginalUrl_ShouldReturnUrl_WhenLinkIsNotExpired() {
        // Given
        String shortCode = "validCode";
        Link validLink = Link.builder()
                .id(1L)
                .shortCode(shortCode)
                .originalUrl("https://google.com")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .isActive(true)
                .build();

        when(valueOperations.get(anyString())).thenReturn(null);
        when(linkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(validLink));

        // When
        String result = linkService.getOriginalUrl(shortCode);

        // Then
        assertEquals("https://google.com", result);
        verify(linkRepository).incrementClickCount(anyLong());
    }
}
