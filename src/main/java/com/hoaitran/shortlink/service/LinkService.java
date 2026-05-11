package com.hoaitran.shortlink.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaitran.shortlink.entity.Link;
import com.hoaitran.shortlink.repository.LinkRepository;
import com.hoaitran.shortlink.utils.Base62Utils;

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

        link = linkRepository.save(link);

        // 2. Dùng Utils để mã hóa ID vừa tạo
        String shortCode = Base62Utils.encode(link.getId());

        // 3. Cập nhật mã chuẩn và lưu lại
        link.setShortCode(shortCode);
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
