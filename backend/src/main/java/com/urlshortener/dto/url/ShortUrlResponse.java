package com.urlshortener.dto.url;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ShortUrlResponse(
    UUID id,
    String shortCode,
    String shortUrl,
    String originalUrl,
    String title,
    String qrCodeDataUrl,
    boolean customAlias,
    boolean active,
    long clickCount,
    OffsetDateTime expiresAt,
    OffsetDateTime createdAt,
    OffsetDateTime lastAccessedAt
) {
}
