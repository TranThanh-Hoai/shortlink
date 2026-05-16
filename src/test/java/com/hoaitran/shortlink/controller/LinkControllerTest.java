package com.hoaitran.shortlink.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.dto.response.LinkResponse;
import com.hoaitran.shortlink.entity.Link;
import com.hoaitran.shortlink.exception.LinkNotFoundException;
import com.hoaitran.shortlink.mapper.LinkMapper;
import com.hoaitran.shortlink.service.LinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LinkControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LinkService linkService;

    @Mock
    private LinkMapper linkMapper;

    @InjectMocks
    private LinkController linkController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(linkController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shorten_ShouldReturnLinkResponse() throws Exception {
        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://example.com");

        Link mockLink = new Link();
        mockLink.setId(1L);
        mockLink.setOriginalUrl("https://example.com");
        mockLink.setShortCode("alias");

        LinkResponse mockResponse = LinkResponse.builder()
                .id(1L)
                .originalUrl("https://example.com")
                .shortCode("alias")
                .build();

        when(linkService.shortenUrl(any(ShortenRequest.class))).thenReturn(mockLink);
        when(linkMapper.toResponse(any(Link.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"))
                .andExpect(jsonPath("$.shortCode").value("alias"));
    }

    @Test
    void redirect_WhenLinkExists_ShouldReturn302() throws Exception {
        String shortCode = "alias";
        String originalUrl = "https://example.com";

        when(linkService.getOriginalUrl(shortCode)).thenReturn(originalUrl);

        mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", originalUrl));
    }

    @Test
    void redirect_WhenLinkDoesNotExist_ShouldReturn404() throws Exception {
        String shortCode = "notfound";

        when(linkService.getOriginalUrl(shortCode)).thenThrow(new LinkNotFoundException("Link not found"));

        mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isNotFound());
    }
}
