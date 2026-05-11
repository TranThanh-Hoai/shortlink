package com.hoaitran.shortlink.mapper;

import com.hoaitran.shortlink.dto.LinkCacheDto;
import com.hoaitran.shortlink.entity.Link;
import org.springframework.stereotype.Component;

@Component
public class LinkMapper {

    public LinkCacheDto toCacheDto(Link link) {
        if (link == null) {
            return null;
        }
        return LinkCacheDto.builder()
                .id(link.getId())
                .originalUrl(link.getOriginalUrl())
                .shortCode(link.getShortCode())
                .isActive(link.isActive())
                .expiresAt(link.getExpiresAt())
                .build();
    }

    public Link toEntity(LinkCacheDto cacheDto) {
        if (cacheDto == null) {
            return null;
        }
        return Link.builder()
                .id(cacheDto.getId())
                .originalUrl(cacheDto.getOriginalUrl())
                .shortCode(cacheDto.getShortCode())
                .isActive(cacheDto.isActive())
                .expiresAt(cacheDto.getExpiresAt())
                .build();
    }
}
