package com.hoaitran.shortlink.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEventResponse {
    private Long id;
    private LocalDateTime clickedAt;
}
