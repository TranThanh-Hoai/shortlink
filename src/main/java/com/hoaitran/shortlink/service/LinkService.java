
package com.hoaitran.shortlink.service;

import com.hoaitran.shortlink.dto.LinkCacheDto;
import com.hoaitran.shortlink.entity.Link;
import com.hoaitran.shortlink.mapper.LinkMapper;
import com.hoaitran.shortlink.repository.LinkRepository;
import com.hoaitran.shortlink.utils.Base62Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final ClickEventService clickEventService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final LinkMapper linkMapper;

    private static final String URL_CACHE_KEY = "shortlink:url:";

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
        link = linkRepository.save(link);

        // 4. Cache vào Redis bằng DTO (hết hạn sau 7 ngày)
        redisTemplate.opsForValue().set(URL_CACHE_KEY + shortCode, linkMapper.toCacheDto(link), 7, TimeUnit.DAYS);

        return link;
    }

    public String getOriginalUrl(String shortCode) {
        // 1. Kiểm tra trong Redis Cache (Lấy DTO)
        LinkCacheDto cacheDto = (LinkCacheDto) redisTemplate.opsForValue().get(URL_CACHE_KEY + shortCode);
        Link link;

        if (cacheDto != null) {
            // Cache Hit -> Chuyển từ DTO sang Entity qua Mapper
            link = linkMapper.toEntity(cacheDto);
        } else {
            // 2. Cache miss -> Tìm trong DB
            link = linkRepository.findByShortCode(shortCode).orElse(null);
            if (link != null) {
                // 3. Lưu vào Cache bằng DTO để dùng cho lần sau
                redisTemplate.opsForValue().set(URL_CACHE_KEY + shortCode, linkMapper.toCacheDto(link), 7, TimeUnit.DAYS);
            }
        }

        if (link != null) {
            // 4. Cập nhật lượt click (Hiện tại vẫn làm đồng bộ vào DB)
            link.setClickCount(link.getClickCount() + 1);
            linkRepository.save(link);

            clickEventService.recordClick(link);

            return link.getOriginalUrl();
        }
        return null;
    }
}
