package com.hoaitran.shortlink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoaitran.shortlink.dto.request.LoginRequest;
import com.hoaitran.shortlink.dto.request.RegisterRequest;
import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.dto.response.AuthResponse;
import com.hoaitran.shortlink.entity.UrlLink;
import com.hoaitran.shortlink.repository.ClickLogRepository;
import com.hoaitran.shortlink.repository.UrlLinkRepository;
import com.hoaitran.shortlink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(properties = {
    "app.ratelimit.enabled=true",
    "app.ratelimit.capacity=5",
    "app.ratelimit.refill-tokens=5",
    "app.ratelimit.refill-minutes=1"
})
@AutoConfigureMockMvc
public class SecurityAndIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private UrlLinkRepository urlLinkRepository;

    @Autowired
    private ClickLogRepository clickLogRepository;

    @MockBean
    private ProxyManager<byte[]> proxyManager;

    @Autowired
    private BucketConfiguration bucketConfiguration;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.hoaitran.shortlink.service.AuthService authService;

    private String user1Token;
    private String user2Token;
    private Long user1Id;
    private Long user2Id;

    @BeforeEach
    void setUp() {
        // Mock proxyManager to always allow by default
        BucketProxy mockBucket = mock(BucketProxy.class);
        when(mockBucket.tryConsume(1)).thenReturn(true);
        RemoteBucketBuilder<byte[]> mockBuilder = mock(RemoteBucketBuilder.class);
        when(proxyManager.builder()).thenReturn(mockBuilder);
        when(mockBuilder.build(any(byte[].class), any(BucketConfiguration.class))).thenReturn(mockBucket);

        clickLogRepository.deleteAllInBatch();
        urlLinkRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // Register two users
        AuthResponse resp1 = authService.register(new RegisterRequest("user1", "user1@test.com", "password"));
        user1Token = resp1.getToken();
        user1Id = userRepository.findByUsername("user1").get().getId();

        AuthResponse resp2 = authService.register(new RegisterRequest("user2", "user2@test.com", "password"));
        user2Token = resp2.getToken();
        user2Id = userRepository.findByUsername("user2").get().getId();
    }

    @Test
    void testMultiUserIsolation() throws Exception {
        // User 1 creates a link
        mockMvc.perform(post("/api/v1/urls/shorten")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ShortenRequest("https://user1.com"))))
                .andExpect(status().isCreated());

        // User 2 creates a link
        mockMvc.perform(post("/api/v1/urls/shorten")
                .header("Authorization", "Bearer " + user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ShortenRequest("https://user2.com"))))
                .andExpect(status().isCreated());

        // User 1 should only see their own link
        mockMvc.perform(get("/api/v1/urls/my-links")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].originalUrl", is("https://user1.com")));

        // User 2 should only see their own link
        mockMvc.perform(get("/api/v1/urls/my-links")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].originalUrl", is("https://user2.com")));
    }

    @Test
    void testAuthorizationAndOwnership() throws Exception {
        // User 1 creates a link
        MvcResult result = mockMvc.perform(post("/api/v1/urls/shorten")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ShortenRequest("https://secret.com"))))
                .andExpect(status().isCreated())
                .andReturn();
        
        String shortCode = objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/shortCode").asText();

        // User 2 tries to delete User 1's link
        mockMvc.perform(delete("/api/v1/urls/" + shortCode)
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());

        // User 2 tries to update status of User 1's link
        mockMvc.perform(patch("/api/v1/urls/" + shortCode + "/status")
                .header("Authorization", "Bearer " + user2Token)
                .param("active", "false"))
                .andExpect(status().isForbidden());

        // User 2 tries to see stats of User 1's link
        mockMvc.perform(get("/api/v1/urls/" + shortCode + "/stats")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
                
        // Unauthenticated user tries to access stats
        mockMvc.perform(get("/api/v1/urls/" + shortCode + "/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCacheInvalidationOnDelete() throws Exception {
        // User 1 creates a link
        MvcResult result = mockMvc.perform(post("/api/v1/urls/shorten")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ShortenRequest("https://to-be-deleted.com"))))
                .andExpect(status().isCreated())
                .andReturn();
        
        String shortCode = objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/shortCode").asText();

        // Redirect once to populate cache
        mockMvc.perform(get("/r/" + shortCode)).andExpect(status().is3xxRedirection());

        // Delete the link
        mockMvc.perform(delete("/api/v1/urls/" + shortCode)
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        // Redirect again, should NOT use cache and return 404
        mockMvc.perform(get("/r/" + shortCode)).andExpect(status().isNotFound());
    }

    @Test
    void testCacheInvalidationOnStatusUpdate() throws Exception {
        String shortCode = "status-check";
        urlLinkRepository.save(UrlLink.builder()
                .originalUrl("https://status.com")
                .shortCode(shortCode)
                .isActive(true)
                .user(userRepository.findById(user1Id).get())
                .build());

        // Redirect to populate cache
        mockMvc.perform(get("/r/" + shortCode)).andExpect(status().is3xxRedirection());
        
        // Deactivate link
        mockMvc.perform(patch("/api/v1/urls/" + shortCode + "/status")
                .header("Authorization", "Bearer " + user1Token)
                .param("active", "false"))
                .andExpect(status().isOk());

        // Redirect again, should be 404
        mockMvc.perform(get("/r/" + shortCode)).andExpect(status().isNotFound());
    }

    @Test
    void testAuthRateLimiting() throws Exception {
        BucketProxy mockBucket = mock(BucketProxy.class);
        RemoteBucketBuilder<byte[]> mockBuilder = mock(RemoteBucketBuilder.class);
        when(proxyManager.builder()).thenReturn(mockBuilder);
        when(mockBuilder.build(any(byte[].class), any(BucketConfiguration.class))).thenReturn(mockBucket);

        // Allow 5 times, then refuse
        when(mockBucket.tryConsume(1))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        LoginRequest loginRequest = new LoginRequest("user1", "password");
        
        // Hit rate limit (5 calls allow)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk());
        }

        // 6th call should be rate limited
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isTooManyRequests());
        
        // Check if config behavior: capacity should be 5 in bucketConfiguration
        assertEquals(5, bucketConfiguration.getBandwidths()[0].getCapacity());
    }
}
