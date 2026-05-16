package com.hoaitran.shortlink.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RateLimitConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean
    public RedisClient redisClient() {
        return RedisClient.create(RedisURI.create(redisHost, redisPort));
    }

    @Bean
    public ProxyManager<byte[]> proxyManager(RedisClient redisClient) {
        StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(ByteArrayCodec.INSTANCE);
        return LettuceBasedProxyManager.builderFor(connection)
                // .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefilling(Duration.ofMinutes(1)))
                .build();
    }
}
