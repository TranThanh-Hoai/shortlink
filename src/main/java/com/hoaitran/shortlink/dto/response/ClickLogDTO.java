package com.hoaitran.shortlink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickLogDTO {
    private String ipAddress;
    private String userAgent;
    private String referer;
    private LocalDateTime clickedAt;
}
