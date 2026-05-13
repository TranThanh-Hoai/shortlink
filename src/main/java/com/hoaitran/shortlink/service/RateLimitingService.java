package com.hoaitran.shortlink.service;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final ProxyManager<byte[]> proxyManager;

    @Value("${app.ratelimit.capacity}")
    private long capacity;

    @Value("${app.ratelimit.tokens-per-minute}")
    private long tokensPerMinute;

    public Bucket resolveBucket(String key) {
        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(tokensPerMinute, Duration.ofMinutes(1)))
                .build();
        return proxyManager.builder().build(key.getBytes(), configSupplier);
    }
}
