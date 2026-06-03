package com.urlshortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrlLifecycleScheduler {

    private final ShortUrlService shortUrlService;

    @Scheduled(cron = "${app.lifecycle.expiration-sync-cron:0 0 2 * * *}", zone = "UTC")
    public void synchronizeExpiredUrls() {
        int updated = shortUrlService.markExpiredUrls();
        log.info("URL lifecycle cleanup marked {} URLs as EXPIRED", updated);
    }
}
