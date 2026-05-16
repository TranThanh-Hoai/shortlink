package com.hoaitran.shortlink.service;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    @Mock
    private ProxyManager<byte[]> proxyManager;

    @Mock
    private RemoteBucketBuilder<byte[]> remoteBucketBuilder;

    @Mock
    private BucketProxy bucketProxy;

    @InjectMocks
    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitingService, "capacity", 10L);
        ReflectionTestUtils.setField(rateLimitingService, "tokensPerMinute", 5L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testResolveBucket() {
        String key = "test-key";
        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        when(remoteBucketBuilder.build(any(byte[].class), any(Supplier.class))).thenReturn(bucketProxy);

        Bucket resolvedBucket = rateLimitingService.resolveBucket(key);

        assertNotNull(resolvedBucket);
        verify(proxyManager).builder();
        verify(remoteBucketBuilder).build(eq(key.getBytes()), any(Supplier.class));
    }
}
