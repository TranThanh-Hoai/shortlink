package com.hoaitran.shortlink.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.hoaitran.shortlink.dto.request.ShortenRequest;
import com.hoaitran.shortlink.entity.Link;
import com.hoaitran.shortlink.service.LinkService;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @PostMapping("/api/shorten")
    public Link shorten(@RequestBody ShortenRequest request) {
        return linkService.shortenUrl(request);
    }

    @GetMapping("/{shortCode}")
    public void redirect(@PathVariable String shortCode, HttpServletResponse response) throws IOException {
        String url = linkService.getOriginalUrl(shortCode);

        if (url != null) {
            response.sendRedirect(url);
        } else {
            response.sendError(404, "Link not found");
        }
    }
}
