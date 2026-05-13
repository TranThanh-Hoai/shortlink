package com.hoaitran.shortlink.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShortenRequest {
    private String url;
    private Long userId;
    private String alias;
    private LocalDateTime expiresAt;
}
