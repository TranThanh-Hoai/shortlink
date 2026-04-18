package com.hoaitran.shortlink.controller;

import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.dto.response.ApiResponse;
import com.hoaitran.shortlink.dto.response.LinkStatsResponse;
import com.hoaitran.shortlink.dto.response.ShortenResponse;
import com.hoaitran.shortlink.entity.UrlLink;
import com.hoaitran.shortlink.service.AnalyticsService;
import com.hoaitran.shortlink.service.QrCodeService;
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
    private final QrCodeService qrCodeService;

    @PostMapping("/shorten")
    public ResponseEntity<ApiResponse<ShortenResponse>> shortenUrl(@Valid @RequestBody ShortenRequest request,
            HttpServletRequest servletRequest) {
        // Anti-abuse: check if original URL is the same domain to prevent loops
        String baseUrl = servletRequest.getRequestURL().toString().replace(servletRequest.getRequestURI(), "");
        if (request.getOriginalUrl().startsWith(baseUrl)) {
            throw new IllegalArgumentException("Cannot shorten URLs from the same domain");
        }

        UrlLink urlLink = urlShortenerService.shortenUrl(request);

        // Construct short URL using a dedicated public path /r/ instead of the API path
        String shortUrl = baseUrl + "/r/" + urlLink.getShortCode();

        ShortenResponse shortenData = ShortenResponse.builder()
                .originalUrl(urlLink.getOriginalUrl())
                .shortCode(urlLink.getShortCode())
                .shortUrl(shortUrl)
                .createdAt(urlLink.getCreatedAt())
                .build();

        ApiResponse<ShortenResponse> response = ApiResponse.<ShortenResponse>builder()
                .timestamp(java.time.LocalDateTime.now())
                .code(HttpStatus.CREATED.value())
                .message("URL shortened successfully")
                .path(servletRequest.getRequestURI())
                .data(shortenData)
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<ApiResponse<Void>> deleteUrl(@PathVariable String shortCode, HttpServletRequest servletRequest) {
        urlShortenerService.deleteUrl(shortCode);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .timestamp(java.time.LocalDateTime.now())
                .code(HttpStatus.NO_CONTENT.value())
                .message("URL deleted successfully")
                .path(servletRequest.getRequestURI())
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{shortCode}/status")
    public ResponseEntity<ApiResponse<UrlLink>> updateStatus(
            @PathVariable String shortCode,
            @RequestParam boolean active,
            HttpServletRequest servletRequest) {
        UrlLink urlLink = urlShortenerService.updateStatus(shortCode, active);
        ApiResponse<UrlLink> response = ApiResponse.<UrlLink>builder()
                .timestamp(java.time.LocalDateTime.now())
                .code(HttpStatus.OK.value())
                .message("URL status updated successfully")
                .path(servletRequest.getRequestURI())
                .data(urlLink)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{shortCode}/stats")
    public ResponseEntity<ApiResponse<LinkStatsResponse>> getStats(
            @PathVariable String shortCode,
            HttpServletRequest servletRequest) {
        LinkStatsResponse stats = analyticsService.getLinkStats(shortCode);
        ApiResponse<LinkStatsResponse> response = ApiResponse.<LinkStatsResponse>builder()
                .timestamp(java.time.LocalDateTime.now())
                .code(HttpStatus.OK.value())
                .message("Stats retrieved successfully")
                .path(servletRequest.getRequestURI())
                .data(stats)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/top")
    public ResponseEntity<ApiResponse<java.util.List<UrlLink>>> getTopLinks(HttpServletRequest servletRequest) {
        java.util.List<UrlLink> topLinks = analyticsService.getTopLinks();
        ApiResponse<java.util.List<UrlLink>> response = ApiResponse.<java.util.List<UrlLink>>builder()
                .timestamp(java.time.LocalDateTime.now())
                .code(HttpStatus.OK.value())
                .message("Top links retrieved successfully")
                .path(servletRequest.getRequestURI())
                .data(topLinks)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/{shortCode}/qr", produces = org.springframework.http.MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(@PathVariable String shortCode, HttpServletRequest servletRequest) {
        // We need the full short URL to encode in QR
        String baseUrl = servletRequest.getRequestURL().toString().replace(servletRequest.getRequestURI(), "");
        String shortUrl = baseUrl + "/r/" + shortCode;
        
        byte[] qrImage = qrCodeService.generateQrCode(shortUrl, 300, 300);
        return ResponseEntity.ok(qrImage);
    }
}
