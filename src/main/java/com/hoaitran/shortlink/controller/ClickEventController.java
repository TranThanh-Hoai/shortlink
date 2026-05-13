package com.hoaitran.shortlink.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.hoaitran.shortlink.entity.ClickEvent;
import com.hoaitran.shortlink.service.ClickEventService;

import java.util.List;

@RestController
@RequestMapping("/api/clicks")
@RequiredArgsConstructor
public class ClickEventController {

    private final ClickEventService clickEventService;

    @GetMapping("/{linkId}")
    public List<ClickEvent> getClicksByLinkId(@PathVariable Long linkId) {
        return clickEventService.getClicksByLinkId(linkId);
    }
}
