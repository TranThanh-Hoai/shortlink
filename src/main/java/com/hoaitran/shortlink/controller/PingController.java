package com.hoaitran.shortlink.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health Check", description = "Endpoints for monitoring service health")
public class PingController {

    @GetMapping("/ping")
    @Operation(summary = "Simple health check", description = "Returns the current status of the service")
    public Map<String, String> ping() {
        return Map.of(
            "status", "UP",
            "message", "Service is healthy",
            "timestamp", String.valueOf(System.currentTimeMillis())
        );
    }
}
