package com.urlshortener.dto.analytics;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record DailyClickPoint(
    LocalDate day,
    long count
) {
}
