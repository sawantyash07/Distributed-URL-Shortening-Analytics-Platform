package com.urlshortener.dto.analytics;

import java.util.List;
import lombok.Builder;

@Builder
public record DashboardSummaryResponse(
    long totalUrls,
    long activeUrls,
    long expiredUrls,
    long totalClicks,
    List<DailyClickPoint> clickTrend,
    List<MetricBreakdown> topUrls
) {
}
