package com.urlshortener.service;

import com.urlshortener.dto.url.CachedShortUrl;
import com.urlshortener.exception.UrlGoneException;
import com.urlshortener.model.UrlStatus;
import com.urlshortener.util.ClientRequestMetadata;
import com.urlshortener.util.ClientRequestMetadataResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RedirectService {

    private final ShortUrlService shortUrlService;
    private final ClickAnalyticsService clickAnalyticsService;
    private final ClientRequestMetadataResolver metadataResolver;

    @Transactional
    public String resolveDestination(String shortCode, HttpServletRequest request) {
        CachedShortUrl cached = shortUrlService.resolveCachedShortUrl(shortCode);
        UrlStatus status = shortUrlService.resolveStatus(cached);
        if (status == UrlStatus.INACTIVE) {
            throw new UrlGoneException("URL is inactive");
        }
        if (status == UrlStatus.EXPIRED) {
            throw new UrlGoneException("URL has expired");
        }
        ClientRequestMetadata metadata = metadataResolver.resolve(request);
        clickAnalyticsService.recordClick(shortCode, metadata);
        return cached.originalUrl();
    }
}
