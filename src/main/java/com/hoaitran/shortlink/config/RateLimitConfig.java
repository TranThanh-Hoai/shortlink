package com.hoaitran.shortlink.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Autowired
    private DataRedisProperties redisProperties;

    @Bean
    public RedisClient redisClient() {
        // Nếu có chuỗi url gộp (giống trên Render), dùng url để khởi tạo kết nối hoàn
        // chỉnh
        if (redisProperties.getUrl() != null) {
            return RedisClient.create(redisProperties.getUrl());
        }
        // Trường hợp chạy local hoặc các cấu hình lẻ, tự động build từ
        // host/port/password có sẵn
        return RedisClient.create(
                io.lettuce.core.RedisURI.builder()
                        .withHost(redisProperties.getHost())
                        .withPort(redisProperties.getPort())
                        .withPassword(redisProperties.getPassword().toCharArray())
                        .withSsl(redisProperties.getSsl().isEnabled())
                        .build());
    }

    @Bean
    public ProxyManager<byte[]> proxyManager(RedisClient redisClient) {
        StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(ByteArrayCodec.INSTANCE);
        return LettuceBasedProxyManager.builderFor(connection).build();
    }
}