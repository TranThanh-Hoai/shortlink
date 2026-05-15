package com.hoaitran.shortlink.controller;

import com.hoaitran.shortlink.dto.response.ClickEventResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hoaitran.shortlink.service.ClickEventService;

@RestController
@RequestMapping("/api/clicks")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Endpoints for viewing link click statistics")
public class ClickEventController {

    private final ClickEventService clickEventService;

    @GetMapping("/{linkId}")
    public ResponseEntity<Page<ClickEventResponse>> getClicksByLinkId(
            @PathVariable Long linkId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(clickEventService.getClicksByLinkId(linkId, pageable));
    }
}
