package com.hoaitran.shortlink.dto.response;

import com.hoaitran.shortlink.entity.ClickLog;
import com.hoaitran.shortlink.entity.UrlLink;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LinkStatsResponse {
    private UrlLink link;
    private List<ClickLog> recentClicks;
}
