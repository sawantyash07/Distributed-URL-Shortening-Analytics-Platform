package com.urlshortener.dto.url;

import java.time.OffsetDateTime;

public record CachedShortUrl(
    String shortCode,
    String originalUrl,
    OffsetDateTime expiresAt,
    String status,
    boolean active
) {
}
