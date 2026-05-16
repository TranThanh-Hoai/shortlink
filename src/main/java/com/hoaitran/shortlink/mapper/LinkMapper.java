package com.hoaitran.shortlink.mapper;

import com.hoaitran.shortlink.dto.LinkCacheDto;
import com.hoaitran.shortlink.dto.response.LinkResponse;
import com.hoaitran.shortlink.entity.Link;
import org.springframework.stereotype.Component;

@Component
public class LinkMapper {

    public LinkResponse toResponse(Link link) {
        if (link == null) {
            return null;
        }
        return LinkResponse.builder()
                .id(link.getId())
                .originalUrl(link.getOriginalUrl())
                .shortCode(link.getShortCode())
                .createdAt(link.getCreatedAt())
                .expiresAt(link.getExpiresAt())
                .clickCount(link.getClickCount())
                .active(link.isActive())
                .build();
    }

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
                .userId(link.getUser() != null ? link.getUser().getId() : null)
                .build();
    }

    public Link toEntity(LinkCacheDto cacheDto) {
        if (cacheDto == null) {
            return null;
        }
        Link link = Link.builder()
                .id(cacheDto.getId())
                .originalUrl(cacheDto.getOriginalUrl())
                .shortCode(cacheDto.getShortCode())
                .isActive(cacheDto.isActive())
                .expiresAt(cacheDto.getExpiresAt())
                .build();
        
        return link;
    }
}
