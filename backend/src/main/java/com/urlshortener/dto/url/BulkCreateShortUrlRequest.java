package com.urlshortener.dto.url;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkCreateShortUrlRequest(
    @Valid @NotEmpty List<CreateShortUrlRequest> urls
) {
}
