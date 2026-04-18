package com.hoaitran.shortlink.controller;

import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.dto.response.ShortenResponse;
import com.hoaitran.shortlink.entity.UrlLink;
import com.hoaitran.shortlink.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequiredArgsConstructor
public class UrlController {
    private final UrlShortenerService urlShortenerService;

    @PostMapping("/api/v1/urls/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(@Valid @RequestBody ShortenRequest request, HttpServletRequest servletRequest) {
        UrlLink urlLink = urlShortenerService.shortenUrl(request.getOriginalUrl());
        
        // Construct base URL from request
        String baseUrl = servletRequest.getRequestURL().toString().replace(servletRequest.getRequestURI(), "");
        String shortUrl = baseUrl + "/" + urlLink.getShortCode();

        ShortenResponse response = ShortenResponse.builder()
                .originalUrl(urlLink.getOriginalUrl())
                .shortCode(urlLink.getShortCode())
                .shortUrl(shortUrl)
                .createdAt(urlLink.getCreatedAt())
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{shortCode}")
    public RedirectView redirect(@PathVariable String shortCode) {
        String originalUrl = urlShortenerService.getOriginalUrl(shortCode);
        return new RedirectView(originalUrl);
    }
}
