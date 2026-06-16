package com.hoaitran.shortlink.service;

import com.hoaitran.shortlink.dto.LinkCacheDto;
import com.hoaitran.shortlink.entity.Link;
import com.hoaitran.shortlink.entity.User;
import com.hoaitran.shortlink.mapper.LinkMapper;
import com.hoaitran.shortlink.repository.LinkRepository;
import com.hoaitran.shortlink.utils.Base62Utils;
import com.hoaitran.shortlink.utils.SnowflakeIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.exception.InvalidAliasException;
import com.hoaitran.shortlink.exception.AliasAlreadyExistsException;
import com.hoaitran.shortlink.exception.LinkExpiredException;

import java.time.LocalDateTime;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final ClickEventService clickEventService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final LinkMapper linkMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final ObjectMapper objectMapper;

    private static final String URL_CACHE_KEY = "shortlink:url:";
    private static final String CLICK_COUNT_CACHE_KEY = "shortlink:clicks:";

    @Transactional
    public Link shortenUrl(ShortenRequest request, User user) {
        String alias = request.getAlias();
        if (alias != null && !alias.isEmpty()) {
            if (!alias.matches("^[a-zA-Z0-9.-]+$")) {
                throw new InvalidAliasException("Alias only allows alphanumeric characters, dot, and hyphen.");
            }
            if (linkRepository.existsByShortCode(alias)) {
                throw new AliasAlreadyExistsException("Alias '" + alias + "' already exists.");
            }
        }

        Link.LinkBuilder linkBuilder = Link.builder()
                .originalUrl(request.getUrl());

        if (user != null) {
            linkBuilder.user(user);
        }

        if (request.getExpiresAt() != null) {
            linkBuilder.expiresAt(request.getExpiresAt());
        }

        Link link = linkBuilder.build();

        if (alias != null && !alias.isEmpty()) {
            // If alias is provided, we still use Snowflake for the primary ID
            link.setId(snowflakeIdGenerator.nextId());
            link.setShortCode(alias);
        } else {
            // Generate Snowflake ID and derive shortCode from it
            long id = snowflakeIdGenerator.nextId();
            link.setId(id);
            link.setShortCode(Base62Utils.encode(id));
        }

        link = linkRepository.save(link);

        cacheLink(link);

        return link;
    }

    @Transactional
    public String getOriginalUrl(String shortCode) {
        Object cachedValue = redisTemplate.opsForValue().get(URL_CACHE_KEY + shortCode);
        LinkCacheDto cacheDto = null;

        if (cachedValue != null) {
            if (cachedValue instanceof LinkCacheDto) {
                cacheDto = (LinkCacheDto) cachedValue;
            } else {
                // Defensive: handle cases where GenericJackson2JsonRedisSerializer returns a LinkedHashMap
                // due to missing type info in Redis or DevTools classloader issues.
                cacheDto = objectMapper.convertValue(cachedValue, LinkCacheDto.class);
            }
        }
        Link link;

        if (cacheDto != null) {
            link = linkMapper.toEntity(cacheDto);
            // Click count is not in CacheDto currently, so we still need to load it or handle it separately
            // To fix the 400 error and bottleneck, let's at least ensure we don't fail here.
        } else {
            link = linkRepository.findByShortCode(shortCode).orElse(null);
            if (link != null) {
                cacheLink(link);
            }
        }

        if (link != null) {
            if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
                redisTemplate.delete(URL_CACHE_KEY + shortCode);
                throw new LinkExpiredException("Link has expired.");
            }
            if (!link.isActive()) {
                throw new LinkExpiredException("Link is inactive.");
            }

            // Atomically increment click count in Redis for tracking
            redisTemplate.opsForValue().increment(CLICK_COUNT_CACHE_KEY + shortCode);

            // Atomically update in DB
            linkRepository.incrementClickCount(link.getId());

            clickEventService.recordClick(link);

            return link.getOriginalUrl();
        }
        return null;
    }

    private void cacheLink(Link link) {
        long ttlInSeconds = TimeUnit.DAYS.toSeconds(7);
        if (link.getExpiresAt() != null) {
            long remainingSeconds = Duration.between(LocalDateTime.now(), link.getExpiresAt()).getSeconds();
            if (remainingSeconds <= 0) {
                return; // already expired
            }
            ttlInSeconds = Math.min(ttlInSeconds, remainingSeconds);
        }
        redisTemplate.opsForValue().set(URL_CACHE_KEY + link.getShortCode(), linkMapper.toCacheDto(link), ttlInSeconds, TimeUnit.SECONDS);
    }
}
