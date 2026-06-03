package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.dto.url.ShortUrlResponse;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.util.QrCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShortUrlMapper {

    private final AppProperties appProperties;
    private final QrCodeGenerator qrCodeGenerator;

    public ShortUrlResponse toResponse(ShortUrl shortUrl) {
        String shortUrlValue = appProperties.getShortBaseUrl() + "/" + shortUrl.getShortCode();
        String status = shortUrl.getStatus() == null ? "ACTIVE" : shortUrl.getStatus().name();
        return ShortUrlResponse.builder()
            .id(shortUrl.getId())
            .shortCode(shortUrl.getShortCode())
            .shortUrl(shortUrlValue)
            .originalUrl(shortUrl.getOriginalUrl())
            .title(shortUrl.getTitle())
            .qrCodeDataUrl(qrCodeGenerator.generateDataUrl(shortUrlValue))
            .customAlias(shortUrl.isCustomAlias())
            .status(status)
            .active(shortUrl.isActive())
            .clickCount(shortUrl.getClickCount())
            .expiresAt(shortUrl.getExpiresAt())
            .createdAt(shortUrl.getCreatedAt())
            .lastAccessedAt(shortUrl.getLastAccessedAt())
            .build();
    }
}
