package com.hoaitran.shortlink.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoaitran.shortlink.dto.LinkCacheDto;
import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.entity.Link;
import com.hoaitran.shortlink.entity.User;
import com.hoaitran.shortlink.exception.AliasAlreadyExistsException;
import com.hoaitran.shortlink.exception.InvalidAliasException;
import com.hoaitran.shortlink.exception.LinkExpiredException;
import com.hoaitran.shortlink.mapper.LinkMapper;
import com.hoaitran.shortlink.repository.LinkRepository;
import com.hoaitran.shortlink.utils.Base62Utils;
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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;
    @Mock
    private ClickEventService clickEventService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private LinkMapper linkMapper;
    @Mock
    private UserService userService;
    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LinkService linkService;

    @BeforeEach
    void setUp() {
        // valueOperations will be returned for any operations
        // Mock redis value operations
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

  @Test
    void testShortenUrl_WithValidAlias_ShouldSucceed() {
        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://example.com");
        request.setAlias("myalias");

        when(linkRepository.existsByShortCode("myalias")).thenReturn(false);
        when(snowflakeIdGenerator.nextId()).thenReturn(12345L);
        when(linkRepository.save(any(Link.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Link savedLink = linkService.shortenUrl(request, null);

        assertNotNull(savedLink);
        assertEquals("https://example.com", savedLink.getOriginalUrl());
        assertEquals("myalias", savedLink.getShortCode());
        assertEquals(12345L, savedLink.getId());

        verify(linkRepository).existsByShortCode("myalias");
        verify(linkRepository).save(any(Link.class));
        verify(valueOperations).set(eq("shortlink:url:myalias"), any(), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void testShortenUrl_WithInvalidAlias_ShouldThrowException() {
        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://example.com");
        request.setAlias("invalid alias@!");

        assertThrows(InvalidAliasException.class, () -> linkService.shortenUrl(request, null));
        verify(linkRepository, never()).save(any());
    }

    @Test
    void testShortenUrl_WithExistingAlias_ShouldThrowException() {
        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://example.com");
        request.setAlias("existingalias");

        when(linkRepository.existsByShortCode("existingalias")).thenReturn(true);

        assertThrows(AliasAlreadyExistsException.class, () -> linkService.shortenUrl(request, null));
        verify(linkRepository, never()).save(any());
    }

    @Test
    void testShortenUrl_WithoutAlias_ShouldGenerateBase62() {
        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://example.com");

        long generatedId = 123456789L;
        when(snowflakeIdGenerator.nextId()).thenReturn(generatedId);
        when(linkRepository.save(any(Link.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Link savedLink = linkService.shortenUrl(request, null);

        assertNotNull(savedLink);
        assertEquals(generatedId, savedLink.getId());
        String expectedShortCode = Base62Utils.encode(generatedId);
        assertEquals(expectedShortCode, savedLink.getShortCode());

        verify(linkRepository).save(any(Link.class));
    }

    @Test
    void testShortenUrl_WithUser_ShouldAssociateUser() {
        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://example.com");
        request.setAlias("useralias");

        User user = new User();
        user.setId(999L);
        user.setUsername("testuser");

        when(linkRepository.existsByShortCode("useralias")).thenReturn(false);
        when(snowflakeIdGenerator.nextId()).thenReturn(54321L);
        when(linkRepository.save(any(Link.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Link savedLink = linkService.shortenUrl(request, user);

        assertNotNull(savedLink);
        assertEquals("https://example.com", savedLink.getOriginalUrl());
        assertEquals("useralias", savedLink.getShortCode());
        assertEquals(user, savedLink.getUser());

        verify(linkRepository).existsByShortCode("useralias");
        verify(linkRepository).save(any(Link.class));
    }

    @Test
    void testGetOriginalUrl_FromCache() {
        String shortCode = "alias";
        LinkCacheDto cacheDto = new LinkCacheDto();
        cacheDto.setOriginalUrl("https://example.com");
        cacheDto.setActive(true);

        Link mockLink = new Link();
        mockLink.setId(1L);
        mockLink.setOriginalUrl("https://example.com");
        mockLink.setActive(true);

        when(valueOperations.get("shortlink:url:" + shortCode)).thenReturn(cacheDto);
        when(linkMapper.toEntity(cacheDto)).thenReturn(mockLink);

        String result = linkService.getOriginalUrl(shortCode);

        assertEquals("https://example.com", result);
        verify(valueOperations).increment("shortlink:clicks:" + shortCode);
        verify(clickEventService).recordClickAndIncrementCount(mockLink);
    }

    @Test
    void testGetOriginalUrl_FromDB() {
        String shortCode = "alias";
        Link mockLink = new Link();
        mockLink.setId(1L);
        mockLink.setOriginalUrl("https://example.com");
        mockLink.setActive(true);
        mockLink.setShortCode(shortCode);

        when(valueOperations.get("shortlink:url:" + shortCode)).thenReturn(null);
        when(linkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(mockLink));

        String result = linkService.getOriginalUrl(shortCode);

        assertEquals("https://example.com", result);
        verify(valueOperations).set(eq("shortlink:url:" + shortCode), any(), anyLong(), eq(TimeUnit.SECONDS));
        verify(valueOperations).increment("shortlink:clicks:" + shortCode);
        verify(clickEventService).recordClickAndIncrementCount(mockLink);
    }

    @Test
    void testGetOriginalUrl_ExpiredLink() {
        String shortCode = "expired";
        Link mockLink = new Link();
        mockLink.setId(1L);
        mockLink.setOriginalUrl("https://example.com");
        mockLink.setActive(true);
        mockLink.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(valueOperations.get("shortlink:url:" + shortCode)).thenReturn(null);
        when(linkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(mockLink));

        assertThrows(LinkExpiredException.class, () -> linkService.getOriginalUrl(shortCode));
        verify(redisTemplate).delete("shortlink:url:" + shortCode);
        verify(clickEventService, never()).recordClick(any());
    }

    @Test
    void testGetOriginalUrl_InactiveLink() {
        String shortCode = "inactive";
        Link mockLink = new Link();
        mockLink.setId(1L);
        mockLink.setOriginalUrl("https://example.com");
        mockLink.setActive(false);

        when(valueOperations.get("shortlink:url:" + shortCode)).thenReturn(null);
        when(linkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(mockLink));

        assertThrows(LinkExpiredException.class, () -> linkService.getOriginalUrl(shortCode));
        verify(clickEventService, never()).recordClick(any());
    }

    @Test
    void testGetOriginalUrl_NotFound() {
        String shortCode = "notfound";
        when(valueOperations.get("shortlink:url:" + shortCode)).thenReturn(null);
        when(linkRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        String result = linkService.getOriginalUrl(shortCode);
        assertNull(result);
    }
}
