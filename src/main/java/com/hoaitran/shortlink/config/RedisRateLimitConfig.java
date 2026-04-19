package com.hoaitran.shortlink.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@lombok.Getter
@ConditionalOnProperty(name = "app.ratelimit.enabled", havingValue = "true", matchIfMissing = true)
public class RedisRateLimitConfig {

    @Value("${app.ratelimit.capacity:10}")
    private int capacity;

    @Value("${app.ratelimit.refill-tokens:10}")
    private int refillTokens;

    @Value("${app.ratelimit.refill-minutes:1}")
    private int refillMinutes;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public RedisClient redisClient() {
        return RedisClient.create(RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort)
                .build());
    }

    @Bean
    public ProxyManager<byte[]> proxyManager(RedisClient redisClient) {
        return LettuceBasedProxyManager.builderFor(redisClient)
                .withExpirationStrategy(io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(refillMinutes)))
                .build();
    }

    @Bean
    public io.github.bucket4j.BucketConfiguration bucketConfiguration() {
        return io.github.bucket4j.BucketConfiguration.builder()
                .addLimit(io.github.bucket4j.Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(refillTokens, Duration.ofMinutes(refillMinutes))
                        .build())
                .build();
    }
}
