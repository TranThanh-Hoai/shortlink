package com.hoaitran.shortlink.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.hoaitran.shortlink.entity.ClickEvent;
import com.hoaitran.shortlink.service.ClickEventService;

import java.util.List;

@RestController
@RequestMapping("/api/clicks")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Endpoints for viewing link click statistics")
public class ClickEventController {

    private final ClickEventService clickEventService;

    @GetMapping("/{linkId}")
    public List<ClickEvent> getClicksByLinkId(@PathVariable Long linkId) {
        return clickEventService.getClicksByLinkId(linkId);
    }
}
