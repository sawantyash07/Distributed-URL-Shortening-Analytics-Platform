package com.urlshortener.service;

import com.urlshortener.model.ClickEvent;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.util.ClientRequestMetadata;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickAnalyticsService {

    private final ShortUrlService shortUrlService;
    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;

    @Async
    @Transactional
    public void recordClick(String shortCode, ClientRequestMetadata metadata) {
        try {
            ShortUrl shortUrl = shortUrlService.getActiveShortUrl(shortCode);
            shortUrl.setClickCount(shortUrl.getClickCount() + 1);
            shortUrl.setLastAccessedAt(OffsetDateTime.now());
            shortUrlRepository.save(shortUrl);
            clickEventRepository.save(ClickEvent.builder()
                .shortUrl(shortUrl)
                .clickedAt(OffsetDateTime.now())
                .ipAddress(metadata.ipAddress())
                .userAgent(metadata.userAgent())
                .browser(metadata.browser())
                .operatingSystem(metadata.operatingSystem())
                .deviceType(metadata.deviceType())
                .country(metadata.country())
                .referrer(metadata.referrer())
                .build());
            shortUrlService.cacheShortUrl(shortUrl);
        } catch (Exception ex) {
            log.warn("Unable to persist click event for code {}", shortCode, ex);
        }
    }
}
