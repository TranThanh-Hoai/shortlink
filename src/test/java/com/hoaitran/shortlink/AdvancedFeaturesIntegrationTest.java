package com.hoaitran.shortlink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.entity.UrlLink;
import com.hoaitran.shortlink.service.AnalyticsService;
import com.hoaitran.shortlink.repository.UrlLinkRepository;
import com.hoaitran.shortlink.repository.ClickLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AdvancedFeaturesIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("urls");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @SpyBean
    private UrlLinkRepository urlLinkRepository;

    @Autowired
    private ClickLogRepository clickLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        clickLogRepository.deleteAllInBatch();
        urlLinkRepository.deleteAllInBatch();
    }

    @Test
    void testDuplicateUrlReusesShortCode() throws Exception {
        String originalUrl = "https://duplicate.example.com";
        ShortenRequest request = new ShortenRequest(originalUrl);

        MvcResult firstResult = mockMvc.perform(post("/api/v1/urls/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String firstCode = objectMapper.readTree(firstResult.getResponse().getContentAsString()).at("/data/shortCode").asText();

        MvcResult secondResult = mockMvc.perform(post("/api/v1/urls/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String secondCode = objectMapper.readTree(secondResult.getResponse().getContentAsString()).at("/data/shortCode").asText();

        assertEquals(firstCode, secondCode);
    }

    @Test
    void testCustomAliasCollision() throws Exception {
        String alias = "my-alias";
        
        mockMvc.perform(post("/api/v1/urls/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ShortenRequest.builder()
                        .originalUrl("https://first.com")
                        .customAlias(alias)
                        .build())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/urls/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ShortenRequest.builder()
                        .originalUrl("https://second.com")
                        .customAlias(alias)
                        .build())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Alias already in use")));
    }

    @Test
    void testConcurrentRedirects() throws Exception {
        String originalUrl = "https://concurrent.example.com";
        UrlLink link = urlLinkRepository.save(UrlLink.builder()
                .originalUrl(originalUrl)
                .shortCode("concurrent1")
                .isActive(true)
                .build());

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    mockMvc.perform(get("/r/" + link.getShortCode()))
                            .andExpect(status().is3xxRedirection());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
    }

    @Test
    void testRedirectionIsCached() throws Exception {
        String originalUrl = "https://cache.test";
        String shortCode = "cache123";
        urlLinkRepository.save(UrlLink.builder()
                .originalUrl(originalUrl)
                .shortCode(shortCode)
                .isActive(true)
                .build());

        mockMvc.perform(get("/r/" + shortCode)).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/r/" + shortCode)).andExpect(status().is3xxRedirection());

        verify(urlLinkRepository, times(1)).findByShortCode(shortCode);
    }

    @Test
    void testExpirationEdgeCases() throws Exception {
        urlLinkRepository.save(UrlLink.builder()
                .originalUrl("https://almost.expired")
                .shortCode("almost1")
                .expiresAt(LocalDateTime.now().plusSeconds(2))
                .isActive(true)
                .build());

        mockMvc.perform(get("/r/almost1")).andExpect(status().is3xxRedirection());

        urlLinkRepository.save(UrlLink.builder()
                .originalUrl("https://just.expired")
                .shortCode("just1")
                .expiresAt(LocalDateTime.now().minusSeconds(2))
                .isActive(true)
                .build());

        mockMvc.perform(get("/r/just1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("expired")));
    }
}
