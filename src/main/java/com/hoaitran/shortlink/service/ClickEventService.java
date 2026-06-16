package com.hoaitran.shortlink.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaitran.shortlink.entity.ClickEvent;
import com.hoaitran.shortlink.entity.Link;
import com.hoaitran.shortlink.repository.ClickEventRepository;
import com.hoaitran.shortlink.repository.LinkRepository;

import com.hoaitran.shortlink.dto.response.ClickEventResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class ClickEventService {

    private final ClickEventRepository clickEventRepository;
    private final LinkRepository linkRepository;

    @Transactional
    public void recordClick(Link link) {
        ClickEvent clickEvent = ClickEvent.builder()
                .link(link)
                .build();
        clickEventRepository.save(clickEvent);
    }

    @Async
    @Transactional
    public void recordClickAndIncrementCount(Link link) {
        linkRepository.incrementClickCount(link.getId());
        ClickEvent clickEvent = ClickEvent.builder()
                .link(link)
                .build();
        clickEventRepository.save(clickEvent);
    }


    public Page<ClickEventResponse> getClicksByLinkId(Long linkId, Pageable pageable) {
        return clickEventRepository.findByLinkId(linkId, pageable)
                .map(click -> ClickEventResponse.builder()
                        .id(click.getId())
                        .clickedAt(click.getClickedAt())
                        .build());
    }
}
