package com.urlshortener.dto.analytics;

import lombok.Builder;

@Builder
public record MetricBreakdown(
    String label,
    long count
) {
}
