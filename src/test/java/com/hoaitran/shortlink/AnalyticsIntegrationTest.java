package com.hoaitran.shortlink;

import com.hoaitran.shortlink.entity.UrlLink;
import com.hoaitran.shortlink.repository.ClickLogRepository;
import com.hoaitran.shortlink.repository.UrlLinkRepository;
import com.hoaitran.shortlink.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UrlLinkRepository urlLinkRepository;

    @Autowired
    private ClickLogRepository clickLogRepository;

    @Autowired
    private UrlShortenerService urlShortenerService;

    @BeforeEach
    void setUp() {
        clickLogRepository.deleteAll();
        urlLinkRepository.deleteAll();
    }

    @Test
    void testClickLoggingAsynchronously() throws Exception {
        // 1. Prepare a link
        String originalUrl = "https://example.com";
        UrlLink urlLink = urlShortenerService.shortenUrl(originalUrl);
        String shortCode = urlLink.getShortCode();

        // 2. Perform redirect
        mockMvc.perform(get("/r/" + shortCode)
                .header("User-Agent", "Test-Agent")
                .header("Referer", "https://referer.com"))
                .andExpect(status().is3xxRedirection());

        // 3. Wait and verify analytics (ClickCount increment and ClickLog creation)
        // Since it's @Async, we use Awaitility to wait for the background task
        await().atMost(Duration.ofSeconds(5))
                .until(() -> urlLinkRepository.findByShortCode(shortCode)
                        .map(UrlLink::getClickCount)
                        .orElse(0L), is(1L));

        await().atMost(Duration.ofSeconds(5))
                .until(() -> clickLogRepository.count(), is(1L));

        var clickLogs = clickLogRepository.findAll();
        assertEquals(1, clickLogs.size());
        var clickLog = clickLogs.get(0);
        // Compare IDs to avoid LazyInitializationException on the proxy
        assertEquals(urlLink.getId(), clickLog.getUrlLink().getId());
        assertEquals("Test-Agent", clickLog.getUserAgent());
        assertEquals("https://referer.com", clickLog.getReferer());
    }
}
