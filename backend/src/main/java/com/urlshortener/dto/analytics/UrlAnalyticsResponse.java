package com.urlshortener.dto.analytics;

import java.util.List;
import lombok.Builder;

@Builder
public record UrlAnalyticsResponse(
    String shortCode,
    long totalClicks,
    List<DailyClickPoint> dailyClicks,
    List<MetricBreakdown> browserBreakdown,
    List<MetricBreakdown> operatingSystemBreakdown,
    List<MetricBreakdown> deviceBreakdown,
    List<MetricBreakdown> countryBreakdown
) {
}
