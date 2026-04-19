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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Autowired
    private com.hoaitran.shortlink.service.AuthService authService;

    @Autowired
    private com.hoaitran.shortlink.repository.UserRepository userRepository;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        clickLogRepository.deleteAll();
        urlLinkRepository.deleteAll();
        userRepository.deleteAll();

        com.hoaitran.shortlink.dto.request.RegisterRequest registerRequest = new com.hoaitran.shortlink.dto.request.RegisterRequest("testuser", "test@test.com", "password");
        com.hoaitran.shortlink.dto.response.AuthResponse authResponse = authService.register(registerRequest);
        jwtToken = authResponse.getToken();
    }

    @Test
    void testShortenAndRedirectFlow() throws Exception {
        String originalUrl = "https://github.com/TranThanh-Hoai";
        ShortenRequest request = new ShortenRequest(originalUrl);

        // 1. Test Shorten API
        MvcResult result = mockMvc.perform(post("/api/v1/urls/shorten")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalUrl", is(originalUrl)))
                .andExpect(jsonPath("$.data.shortCode", notNullValue()))
                .andExpect(jsonPath("$.data.shortUrl", containsString("/r/")))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String shortCode = objectMapper.readTree(responseBody).get("data").get("shortCode").asText();

        // 2. Test Redirect API
        mockMvc.perform(get("/r/" + shortCode))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", originalUrl));

        // 2.1 Test Old Redirect Path returns 403 (Forbidden/Unauthorized) because it's an authenticated endpoint and we didn't pass JWT here
        mockMvc.perform(get("/api/v1/urls/" + shortCode))
                .andExpect(status().isForbidden());

        // 3. Test 404 for invalid code
        mockMvc.perform(get("/r/invalidCode"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Short URL not found")));
    }

    @Test
    void testShortenValidation() throws Exception {
        ShortenRequest invalidRequest = new ShortenRequest("invalid-url");

        mockMvc.perform(post("/api/v1/urls/shorten")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("originalUrl")));
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
                .andExpect(jsonPath("$.message", containsString("expired")));
    }

    @Test
    void testCustomAliasStatusDeleteStatsTopAndQr() throws Exception {
        ShortenRequest request = ShortenRequest.builder()
                .originalUrl("https://spring.io")
                .customAlias("springdocs")
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.shortCode", is("springdocs")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.code", is(201)))
                .andExpect(jsonPath("$.path", is("/api/v1/urls/shorten")));

        mockMvc.perform(patch("/api/v1/urls/springdocs/status")
                .header("Authorization", "Bearer " + jwtToken)
                .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active", is(false)))
                .andExpect(jsonPath("$.code", is(200)));

        mockMvc.perform(get("/api/v1/urls/springdocs/stats").header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.link.shortCode", is("springdocs")));

        mockMvc.perform(get("/api/v1/urls/top").header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].shortCode", is("springdocs")));

        MvcResult qrResult = mockMvc.perform(get("/api/v1/urls/springdocs/qr").header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn();
        assertTrue(qrResult.getResponse().getContentAsByteArray().length > 0);

        mockMvc.perform(delete("/api/v1/urls/springdocs").header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.message", is("URL deleted successfully")));
    }

    @Test
    void testIdempotencyKeyReturnsSameLinkForSameRequestAndConflictsForDifferentRequest() throws Exception {
        ShortenRequest request = ShortenRequest.builder()
                .originalUrl("https://example.com/idempotent")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        MvcResult first = mockMvc.perform(post("/api/v1/urls/shorten")
                .header("Authorization", "Bearer " + jwtToken)
                .header("Idempotency-Key", "idem-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/v1/urls/shorten")
                .header("Authorization", "Bearer " + jwtToken)
                .header("Idempotency-Key", "idem-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstCode = objectMapper.readTree(first.getResponse().getContentAsString()).at("/data/shortCode").asText();
        String secondCode = objectMapper.readTree(second.getResponse().getContentAsString()).at("/data/shortCode").asText();
        assertEquals(firstCode, secondCode);

        ShortenRequest differentRequest = ShortenRequest.builder()
                .originalUrl("https://example.com/other")
                .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
                .header("Authorization", "Bearer " + jwtToken)
                .header("Idempotency-Key", "idem-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(differentRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is(409)))
                .andExpect(jsonPath("$.message", containsString("Idempotency-Key")));
    }
}
