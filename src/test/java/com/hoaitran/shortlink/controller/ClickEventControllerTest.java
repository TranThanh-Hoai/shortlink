package com.hoaitran.shortlink.controller;

import com.hoaitran.shortlink.dto.response.ClickEventResponse;
import com.hoaitran.shortlink.service.ClickEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClickEventControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClickEventService clickEventService;

    @InjectMocks
    private ClickEventController clickEventController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clickEventController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getClicksByLinkId_ShouldReturnPageOfClicks() throws Exception {
        Long linkId = 1L;

        ClickEventResponse mockResponse = ClickEventResponse.builder()
                .id(100L)
                .clickedAt(LocalDateTime.now())
                .build();

        PageImpl<ClickEventResponse> page = new PageImpl<>(Collections.singletonList(mockResponse), org.springframework.data.domain.PageRequest.of(0, 10), 1);

        when(clickEventService.getClicksByLinkId(eq(linkId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/clicks/{linkId}", linkId)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
