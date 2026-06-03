package com.urlshortener.dto.url;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record CreateShortUrlRequest(
    @Size(max = 255) String title,
    @Pattern(regexp = "https?://.+", message = "Original URL must start with http:// or https://") String originalUrl,
    @Pattern(regexp = "^[a-zA-Z0-9_-]{4,32}$", message = "Alias must be 4-32 characters and contain only letters, digits, hyphen, or underscore")
    String customAlias,
    OffsetDateTime expiresAt
) {
}
