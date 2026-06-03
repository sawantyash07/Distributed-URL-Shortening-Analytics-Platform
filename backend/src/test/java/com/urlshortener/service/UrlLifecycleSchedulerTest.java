package com.urlshortener.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UrlLifecycleSchedulerTest {

    @Mock
    private ShortUrlService shortUrlService;

    @Test
    void shouldRunExpirationSynchronization() {
        when(shortUrlService.markExpiredUrls()).thenReturn(4);
        UrlLifecycleScheduler scheduler = new UrlLifecycleScheduler(shortUrlService);

        scheduler.synchronizeExpiredUrls();

        verify(shortUrlService).markExpiredUrls();
    }
}
