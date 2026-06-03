package com.urlshortener.dto.url;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record UpdateShortUrlRequest(
    @Size(max = 255) String title,
    OffsetDateTime expiresAt,
    Boolean active
) {
}
