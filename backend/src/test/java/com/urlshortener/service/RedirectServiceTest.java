package com.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.urlshortener.dto.url.CachedShortUrl;
import com.urlshortener.exception.UrlGoneException;
import com.urlshortener.util.ClientRequestMetadata;
import com.urlshortener.util.ClientRequestMetadataResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RedirectServiceTest {

    @Mock
    private ShortUrlService shortUrlService;
    @Mock
    private ClickAnalyticsService clickAnalyticsService;
    @Mock
    private ClientRequestMetadataResolver metadataResolver;
    @Mock
    private HttpServletRequest request;

    @Test
    void shouldBlockInactiveUrls() {
        RedirectService redirectService = new RedirectService(shortUrlService, clickAnalyticsService, metadataResolver);
        CachedShortUrl cached = new CachedShortUrl("abc123", "https://example.com", OffsetDateTime.now().plusDays(1), "INACTIVE", false);
        when(shortUrlService.resolveCachedShortUrl("abc123")).thenReturn(cached);
        when(shortUrlService.resolveStatus(cached)).thenReturn(com.urlshortener.model.UrlStatus.INACTIVE);

        assertThatThrownBy(() -> redirectService.resolveDestination("abc123", request))
            .isInstanceOf(UrlGoneException.class)
            .hasMessage("URL is inactive");

        verify(clickAnalyticsService, never()).recordClick(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldBlockExpiredUrls() {
        RedirectService redirectService = new RedirectService(shortUrlService, clickAnalyticsService, metadataResolver);
        CachedShortUrl cached = new CachedShortUrl("abc123", "https://example.com", OffsetDateTime.now().minusDays(1), "EXPIRED", true);
        when(shortUrlService.resolveCachedShortUrl("abc123")).thenReturn(cached);
        when(shortUrlService.resolveStatus(cached)).thenReturn(com.urlshortener.model.UrlStatus.EXPIRED);

        assertThatThrownBy(() -> redirectService.resolveDestination("abc123", request))
            .isInstanceOf(UrlGoneException.class)
            .hasMessage("URL has expired");

        verify(clickAnalyticsService, never()).recordClick(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRedirectActiveUrls() {
        RedirectService redirectService = new RedirectService(shortUrlService, clickAnalyticsService, metadataResolver);
        CachedShortUrl cached = new CachedShortUrl("abc123", "https://example.com", OffsetDateTime.now().plusDays(1), "ACTIVE", true);
        ClientRequestMetadata metadata = new ClientRequestMetadata("127.0.0.1", "agent", "Chrome", "Windows", "Desktop", "Local", null);
        when(shortUrlService.resolveCachedShortUrl("abc123")).thenReturn(cached);
        when(shortUrlService.resolveStatus(cached)).thenReturn(com.urlshortener.model.UrlStatus.ACTIVE);
        when(metadataResolver.resolve(request)).thenReturn(metadata);

        redirectService.resolveDestination("abc123", request);

        verify(clickAnalyticsService).recordClick("abc123", metadata);
    }
}
