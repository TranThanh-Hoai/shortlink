package com.hoaitran.shortlink.controller;

import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.dto.response.LinkResponse;
import com.hoaitran.shortlink.entity.User;
import com.hoaitran.shortlink.exception.LinkNotFoundException;
import com.hoaitran.shortlink.mapper.LinkMapper;
import com.hoaitran.shortlink.security.CustomUserDetails;
import com.hoaitran.shortlink.service.LinkService;
import com.hoaitran.shortlink.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Tag(name = "Link Management", description = "Endpoints for creating and managing short links")
public class LinkController {

    private final LinkService linkService;
    private final LinkMapper linkMapper;
    private final QrCodeService qrCodeService;

    @PostMapping("/api/shorten")
    @Operation(summary = "Create a short link", description = "Generates a short link for the given URL and includes a QR code (Base64).")
    public ResponseEntity<LinkResponse> shorten(
            @Valid @RequestBody ShortenRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest servletRequest) {
        User user = userDetails != null ? userDetails.getUser() : null;
        var link = linkService.shortenUrl(request, user);
        LinkResponse response = linkMapper.toResponse(link);
        
        // Generate QR code for the short link
        String baseUrl = servletRequest.getScheme() + "://" + servletRequest.getServerName();
        if (servletRequest.getServerPort() != 80 && servletRequest.getServerPort() != 443) {
            baseUrl += ":" + servletRequest.getServerPort();
        }
        String shortUrl = baseUrl + "/" + link.getShortCode();
        
        response.setQrCode(qrCodeService.generateQrCodeBase64(shortUrl, 300, 300));
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shortCode}/qr")
    @Operation(summary = "Get QR code image", description = "Returns the QR code image for the given short code.")
    public ResponseEntity<byte[]> getQrCode(@PathVariable String shortCode, HttpServletRequest servletRequest) {
        String baseUrl = servletRequest.getScheme() + "://" + servletRequest.getServerName();
        if (servletRequest.getServerPort() != 80 && servletRequest.getServerPort() != 443) {
            baseUrl += ":" + servletRequest.getServerPort();
        }
        String shortUrl = baseUrl + "/" + shortCode;
        
        byte[] qrImage = qrCodeService.generateQrCodeImage(shortUrl, 300, 300);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String url = linkService.getOriginalUrl(shortCode);

        if (url != null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(url))
                    .build();
        } else {
            throw new LinkNotFoundException("Link not found");
        }
    }
}
