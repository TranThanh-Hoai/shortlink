package com.hoaitran.shortlink.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LinkStatsResponse {
    private UrlResponseDTO link;
    private List<ClickLogDTO> recentClicks;
}
