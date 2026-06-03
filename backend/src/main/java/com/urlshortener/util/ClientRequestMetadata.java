package com.urlshortener.util;

public record ClientRequestMetadata(
    String ipAddress,
    String userAgent,
    String browser,
    String operatingSystem,
    String deviceType,
    String country,
    String referrer
) {
}
