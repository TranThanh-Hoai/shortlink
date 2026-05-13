package com.hoaitran.shortlink.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaitran.shortlink.entity.Link;
import com.hoaitran.shortlink.repository.LinkRepository;
import com.hoaitran.shortlink.utils.Base62Utils;

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

    @Transactional
    public Link shortenUrl(ShortenRequest request) {
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

        if (request.getUserId() != null) {
            userService.findById(request.getUserId()).ifPresent(user -> linkBuilder.user(user));
        }

        if (request.getExpiresAt() != null) {
            linkBuilder.expiresAt(request.getExpiresAt());
        }

        Link link = linkBuilder.build();

        if (alias != null && !alias.isEmpty()) {
            link.setShortCode(alias);
            link = linkRepository.save(link);
        } else {
            link = linkRepository.save(link);
            String shortCode = Base62Utils.encode(link.getId());
            link.setShortCode(shortCode);
            link = linkRepository.save(link);
        }

        cacheLink(link);

        return link;
    }

    public String getOriginalUrl(String shortCode) {
        LinkCacheDto cacheDto = (LinkCacheDto) redisTemplate.opsForValue().get(URL_CACHE_KEY + shortCode);
        Link link;

        if (cacheDto != null) {
            link = linkMapper.toEntity(cacheDto);
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

            link.setClickCount(link.getClickCount() + 1);
            linkRepository.save(link);

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
