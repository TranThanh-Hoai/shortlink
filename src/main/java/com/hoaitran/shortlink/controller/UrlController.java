package com.hoaitran.shortlink.controller;

import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.dto.response.ApiResponse;
import com.hoaitran.shortlink.dto.response.ApiResponseFactory;
import com.hoaitran.shortlink.dto.response.LinkStatsResponse;
import com.hoaitran.shortlink.dto.response.ShortenResponse;
import com.hoaitran.shortlink.dto.response.UrlResponseDTO;
import com.hoaitran.shortlink.entity.UrlLink;
import com.hoaitran.shortlink.entity.User;
import com.hoaitran.shortlink.service.AnalyticsService;
import com.hoaitran.shortlink.service.CustomUserDetails;
import com.hoaitran.shortlink.service.QrCodeService;
import com.hoaitran.shortlink.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {
    private final UrlShortenerService urlShortenerService;
    private final AnalyticsService analyticsService;
    private final QrCodeService qrCodeService;

    private User getCurrentUser(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new org.springframework.security.access.AccessDeniedException("User not authenticated");
        }
        return userDetails.getUser();
    }

    @PostMapping("/shorten")
    public ResponseEntity<ApiResponse<ShortenResponse>> shortenUrl(@Valid @RequestBody ShortenRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest servletRequest) {
        
        User user = getCurrentUser(userDetails);

        // Anti-abuse: check if original URL is the same domain to prevent loops
        String requestUrl = servletRequest.getRequestURL().toString();
        try {
            java.net.URI originalUri = new java.net.URI(request.getOriginalUrl());
            java.net.URI baseUri = new java.net.URI(requestUrl);
            
            String originalHost = originalUri.getHost();
            String baseHost = baseUri.getHost();

            if (originalHost != null && originalHost.equalsIgnoreCase(baseHost)) {
                throw new IllegalArgumentException("Cannot shorten URLs from the same domain");
            }
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format");
        }

        UrlLink urlLink = urlShortenerService.shortenUrl(request, idempotencyKey, user);

        // Construct short URL using a dedicated public path /r/ instead of the API path
        String baseUrl = requestUrl.replace(servletRequest.getRequestURI(), "");
        String shortUrl = baseUrl + "/r/" + urlLink.getShortCode();

        ShortenResponse shortenData = ShortenResponse.builder()
                .originalUrl(urlLink.getOriginalUrl())
                .shortCode(urlLink.getShortCode())
                .shortUrl(shortUrl)
                .createdAt(urlLink.getCreatedAt())
                .build();

        ApiResponse<ShortenResponse> response = ApiResponseFactory.success(
                HttpStatus.CREATED,
                "URL shortened successfully",
                servletRequest,
                shortenData);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<ApiResponse<Void>> deleteUrl(
            @PathVariable String shortCode, 
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest servletRequest) {
        User user = getCurrentUser(userDetails);
        urlShortenerService.deleteUrl(shortCode, user);
        
        ApiResponse<Void> response = ApiResponseFactory.success(
                HttpStatus.OK,
                "URL deleted successfully",
                servletRequest,
                null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{shortCode}/status")
    public ResponseEntity<ApiResponse<UrlResponseDTO>> updateStatus(
            @PathVariable String shortCode,
            @RequestParam boolean active,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest servletRequest) {
        User user = getCurrentUser(userDetails);
        UrlLink urlLink = urlShortenerService.updateStatus(shortCode, active, user);
        
        String baseUrl = servletRequest.getRequestURL().toString().replace(servletRequest.getRequestURI(), "");
        UrlResponseDTO responseData = urlShortenerService.mapToUrlDTO(urlLink, baseUrl);

        ApiResponse<UrlResponseDTO> response = ApiResponseFactory.success(
                HttpStatus.OK,
                "URL status updated successfully",
                servletRequest,
                responseData);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{shortCode}/stats")
    public ResponseEntity<ApiResponse<LinkStatsResponse>> getStats(
            @PathVariable String shortCode,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest servletRequest) {
        User user = getCurrentUser(userDetails);
        String baseUrl = servletRequest.getRequestURL().toString().replace(servletRequest.getRequestURI(), "");
        
        LinkStatsResponse stats = analyticsService.getLinkStats(shortCode, user, baseUrl);
        
        ApiResponse<LinkStatsResponse> response = ApiResponseFactory.success(
                HttpStatus.OK,
                "Stats retrieved successfully",
                servletRequest,
                stats);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/top")
    public ResponseEntity<ApiResponse<java.util.List<UrlResponseDTO>>> getTopLinks(HttpServletRequest servletRequest) {
        String baseUrl = servletRequest.getRequestURL().toString().replace(servletRequest.getRequestURI(), "");
        java.util.List<UrlResponseDTO> topLinks = analyticsService.getTopLinks(baseUrl);
        ApiResponse<java.util.List<UrlResponseDTO>> response = ApiResponseFactory.success(
                HttpStatus.OK,
                "Top links retrieved successfully",
                servletRequest,
                topLinks);
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

    @GetMapping("/my-links")
    public ResponseEntity<ApiResponse<java.util.List<UrlResponseDTO>>> getMyLinks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest servletRequest) {
        User user = getCurrentUser(userDetails);
        String baseUrl = servletRequest.getRequestURL().toString().replace(servletRequest.getRequestURI(), "");
        java.util.List<UrlResponseDTO> myLinks = urlShortenerService.getUserLinks(user, baseUrl);
        
        ApiResponse<java.util.List<UrlResponseDTO>> response = ApiResponseFactory.success(
                HttpStatus.OK,
                "User links retrieved successfully",
                servletRequest,
                myLinks);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
