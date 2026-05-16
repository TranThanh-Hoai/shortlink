package com.hoaitran.shortlink.security;

import com.hoaitran.shortlink.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "app.ratelimit.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String key = resolveLimitKey(request);
        Bucket bucket = rateLimitingService.resolveBucket(key);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for key: {} on path: {}", key, request.getRequestURI());
            response.setStatus(429);
            response.setHeader("Content-Type", "text/plain; charset=utf-8");
            response.getWriter().write("Quá nhiều yêu cầu. Vui lòng thử lại sau.");
        }
    }

    private String resolveLimitKey(HttpServletRequest request) {
        // Prioritize Authorization header (JWT) as the key for authenticated users
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader; 
        }

        // Fallback to IP address for public endpoints (Register, Redirect)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        return request.getRemoteAddr();
    }
}
