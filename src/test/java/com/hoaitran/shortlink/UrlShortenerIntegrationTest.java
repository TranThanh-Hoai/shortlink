package com.hoaitran.shortlink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.entity.UrlLink;
import com.hoaitran.shortlink.repository.UrlLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UrlShortenerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UrlLinkRepository urlLinkRepository;

    @Autowired
    private com.hoaitran.shortlink.repository.ClickLogRepository clickLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        clickLogRepository.deleteAll();
        urlLinkRepository.deleteAll();
    }

    @Test
    void testShortenAndRedirectFlow() throws Exception {
        String originalUrl = "https://github.com/TranThanh-Hoai";
        ShortenRequest request = new ShortenRequest(originalUrl);

        // 1. Test Shorten API
        MvcResult result = mockMvc.perform(post("/api/v1/urls/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalUrl", is(originalUrl)))
                .andExpect(jsonPath("$.shortCode", notNullValue()))
                .andExpect(jsonPath("$.shortUrl", containsString("/r/")))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String shortCode = objectMapper.readTree(responseBody).get("shortCode").asText();

        // 2. Test Redirect API
        mockMvc.perform(get("/r/" + shortCode))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", originalUrl));

        // 2.1 Test Old Redirect Path returns 404
        mockMvc.perform(get("/api/v1/urls/" + shortCode))
                .andExpect(status().isNotFound());

        // 3. Test 404 for invalid code
        mockMvc.perform(get("/r/invalidCode"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Short URL not found")));
    }

    @Test
    void testShortenValidation() throws Exception {
        ShortenRequest invalidRequest = new ShortenRequest("invalid-url");

        mockMvc.perform(post("/api/v1/urls/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.originalUrl", notNullValue()));
    }

    @Test
    void testRedirectReturnsNotFoundForExpiredLink() throws Exception {
        UrlLink expiredLink = urlLinkRepository.save(UrlLink.builder()
                .originalUrl("https://expired.example.com")
                .shortCode("expired1")
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .isActive(true)
                .build());

        mockMvc.perform(get("/r/" + expiredLink.getShortCode()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("expired")));
    }
}
