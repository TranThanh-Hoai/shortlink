package com.hoaitran.shortlink.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ProxyManager<byte[]> proxyManager;
    private final BucketConfiguration bucketConfiguration;
    private final ObjectMapper objectMapper;

    @Value("${app.ratelimit.trusted-proxies:}")
    private String trustedProxies;

    private static final String SHORTEN_API = "/api/v1/urls/shorten";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith(SHORTEN_API)) {
            String clientIp = getClientIp(request);
            byte[] clientIpBytes = clientIp.getBytes(StandardCharsets.UTF_8);

            // Lấy bucket từ Redis thông qua ProxyManager sử dụng config đã được cache
            Bucket bucket = proxyManager.builder().build(clientIpBytes, bucketConfiguration);

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json; charset=UTF-8");

                ErrorResponse error = new ErrorResponse(
                        "Too many requests",
                        "Bạn đang thao tác quá nhanh, vui lòng thử lại sau giây lát!");

                objectMapper.writeValue(response.getWriter(), error);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (trustedProxies == null || trustedProxies.isEmpty()) {
            // Nếu không có proxy tin cậy, dùng IP trực tiếp từ request
            return remoteAddr;
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor == null || xForwardedFor.isEmpty()) {
            return remoteAddr;
        }

        // Kiểm tra xem remoteAddr có nằm trong danh sách trusted proxies không
        boolean isTrusted = java.util.Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .anyMatch(ip -> ip.equals(remoteAddr));

        if (isTrusted) {
            // Nếu tin cậy sender (proxy), lấy IP đầu tiên trong chuỗi forwarded
            return xForwardedFor.split(",")[0].trim();
        }

        return remoteAddr;
    }

    record ErrorResponse(String error, String message) {
    }
}