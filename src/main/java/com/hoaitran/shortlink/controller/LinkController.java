package com.hoaitran.shortlink.controller;

import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.dto.response.LinkResponse;
import com.hoaitran.shortlink.exception.LinkNotFoundException;
import com.hoaitran.shortlink.mapper.LinkMapper;
import com.hoaitran.shortlink.service.LinkService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Tag(name = "Link Management", description = "Endpoints for creating and managing short links")
public class LinkController {

    private final LinkService linkService;
    private final LinkMapper linkMapper;

    @PostMapping("/api/shorten")
    public ResponseEntity<LinkResponse> shorten(@RequestBody ShortenRequest request) {
        return ResponseEntity.ok(linkMapper.toResponse(linkService.shortenUrl(request)));
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
