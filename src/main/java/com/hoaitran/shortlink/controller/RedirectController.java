package com.hoaitran.shortlink.controller;

import com.hoaitran.shortlink.service.AnalyticsService;
import com.hoaitran.shortlink.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequiredArgsConstructor
public class RedirectController {
    private final UrlShortenerService urlShortenerService;
    private final AnalyticsService analyticsService;

    @GetMapping("/r/{shortCode}")
    public RedirectView redirect(@PathVariable String shortCode, HttpServletRequest request) {
        String originalUrl = urlShortenerService.getOriginalUrl(shortCode);

        // Record click asynchronously
        analyticsService.recordClick(
                shortCode,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                request.getHeader("Referer"));

        return new RedirectView(originalUrl);
    }
}
