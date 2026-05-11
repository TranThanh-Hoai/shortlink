package com.hoaitran.shortlink.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaitran.shortlink.entity.ClickEvent;
import com.hoaitran.shortlink.entity.Link;
import com.hoaitran.shortlink.repository.ClickEventRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClickEventService {

    private final ClickEventRepository clickEventRepository;

    @Transactional
    public void recordClick(Link link) {
        ClickEvent clickEvent = ClickEvent.builder()
                .link(link)
                .build();
        clickEventRepository.save(clickEvent);
    }

    public List<ClickEvent> getClicksByLinkId(Long linkId) {
        return clickEventRepository.findByLinkId(linkId);
    }
}
