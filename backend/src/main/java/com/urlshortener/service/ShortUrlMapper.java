package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.dto.url.ShortUrlResponse;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.util.NetworkUtils;
import com.urlshortener.util.QrCodeGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class ShortUrlMapper {

    private final AppProperties appProperties;
    private final QrCodeGenerator qrCodeGenerator;

    public ShortUrlResponse toResponse(ShortUrl shortUrl) {
        String baseUrl = resolveScannableBaseUrl();
        String shortUrlValue = baseUrl + "/" + shortUrl.getShortCode();
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

    private String resolveScannableBaseUrl() {
        String configuredBaseUrl = appProperties.getShortBaseUrl();
        if (configuredBaseUrl == null || configuredBaseUrl.isBlank()) {
            configuredBaseUrl = "http://localhost:8080/r";
        }

        if (configuredBaseUrl.contains("localhost") || configuredBaseUrl.contains("127.0.0.1")) {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String hostHeader = request.getHeader("X-Forwarded-Host");
                if (hostHeader == null || hostHeader.isBlank()) {
                    hostHeader = request.getHeader("Host");
                }
                if (hostHeader != null && !hostHeader.isBlank() && !hostHeader.contains("localhost") && !hostHeader.contains("127.0.0.1")) {
                    String scheme = request.getHeader("X-Forwarded-Proto");
                    if (scheme == null || scheme.isBlank()) {
                        scheme = request.getScheme();
                    }
                    return scheme + "://" + hostHeader + "/r";
                }
            }
            String lanIp = NetworkUtils.getLocalLanIp();
            return configuredBaseUrl.replace("localhost", lanIp).replace("127.0.0.1", lanIp);
        }

        return configuredBaseUrl;
    }
}

