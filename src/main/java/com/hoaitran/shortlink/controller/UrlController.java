package com.hoaitran.shortlink.controller;

import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.dto.response.ShortenResponse;
import com.hoaitran.shortlink.entity.UrlLink;
import com.hoaitran.shortlink.service.AnalyticsService;
import com.hoaitran.shortlink.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {
    private final UrlShortenerService urlShortenerService;
    private final AnalyticsService analyticsService;

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(@Valid @RequestBody ShortenRequest request,
            HttpServletRequest servletRequest) {
        UrlLink urlLink = urlShortenerService.shortenUrl(request.getOriginalUrl());

        // Construct short URL using a dedicated public path /r/ instead of the API path
        String baseUrl = servletRequest.getRequestURL().toString().replace(servletRequest.getRequestURI(), "");
        String shortUrl = baseUrl + "/r/" + urlLink.getShortCode();

        ShortenResponse response = ShortenResponse.builder()
                .originalUrl(urlLink.getOriginalUrl())
                .shortCode(urlLink.getShortCode())
                .shortUrl(shortUrl)
                .createdAt(urlLink.getCreatedAt())
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}
