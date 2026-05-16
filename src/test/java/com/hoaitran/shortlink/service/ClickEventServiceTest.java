package com.hoaitran.shortlink.service;

import com.hoaitran.shortlink.dto.response.ClickEventResponse;
import com.hoaitran.shortlink.entity.ClickEvent;
import com.hoaitran.shortlink.entity.Link;
import com.hoaitran.shortlink.repository.ClickEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClickEventServiceTest {

    @Mock
    private ClickEventRepository clickEventRepository;

    @InjectMocks
    private ClickEventService clickEventService;

    @Test
    void testRecordClick() {
        Link mockLink = new Link();
        mockLink.setId(1L);

        clickEventService.recordClick(mockLink);

        verify(clickEventRepository).save(any(ClickEvent.class));
    }

    @Test
    void testGetClicksByLinkId() {
        Long linkId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        ClickEvent mockClick = new ClickEvent();
        mockClick.setId(100L);
        mockClick.setClickedAt(LocalDateTime.now());
        
        Page<ClickEvent> clickPage = new PageImpl<>(Collections.singletonList(mockClick));

        when(clickEventRepository.findByLinkId(linkId, pageable)).thenReturn(clickPage);

        Page<ClickEventResponse> responsePage = clickEventService.getClicksByLinkId(linkId, pageable);

        assertNotNull(responsePage);
        assertEquals(1, responsePage.getTotalElements());
        assertEquals(100L, responsePage.getContent().get(0).getId());

        verify(clickEventRepository).findByLinkId(linkId, pageable);
    }
}
