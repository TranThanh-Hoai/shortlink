package com.hoaitran.shortlink.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkCacheDto implements Serializable {
    private Long id;
    private String originalUrl;
    private String shortCode;
    private boolean isActive;
    private LocalDateTime expiresAt;
}
