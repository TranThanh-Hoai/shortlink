package com.hoaitran.shortlink.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaitran.shortlink.entity.Link;
import com.hoaitran.shortlink.repository.LinkRepository;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;

    @Transactional
    public Link shortenUrl(String originalUrl) {
        // 1. Tạo đối tượng Link
        Link link = Link.builder()
                .originalUrl(originalUrl)
                .build();

        return linkRepository.save(link);
    }

    public String getOriginalUrl(String shortCode) {
        Link link = linkRepository.findByShortCode(shortCode).orElse(null);
        if (link != null) {
            link.setClickCount(link.getClickCount() + 1);
            linkRepository.save(link);
            return link.getOriginalUrl();
        }
        return null;
    }
}
