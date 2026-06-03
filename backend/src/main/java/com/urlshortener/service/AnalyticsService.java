package com.urlshortener.service;

import com.urlshortener.dto.analytics.DailyClickPoint;
import com.urlshortener.dto.analytics.DashboardSummaryResponse;
import com.urlshortener.dto.analytics.MetricBreakdown;
import com.urlshortener.dto.analytics.UrlAnalyticsResponse;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.exception.UnauthorizedOperationException;
import com.urlshortener.model.AppUser;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortUrlRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;

    @Transactional(readOnly = true)
    public UrlAnalyticsResponse getUrlAnalytics(AppUser user, UUID shortUrlId) {
        ShortUrl shortUrl = shortUrlRepository.findById(shortUrlId)
            .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        if (!shortUrl.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedOperationException("You can only view analytics for your own URLs");
        }

        return UrlAnalyticsResponse.builder()
            .shortCode(shortUrl.getShortCode())
            .totalClicks(shortUrl.getClickCount())
            .dailyClicks(clickEventRepository.findDailyClicks(shortUrlId).stream().map(item ->
                DailyClickPoint.builder().day(item.getDay()).count(item.getCount()).build()
            ).toList())
            .browserBreakdown(mapBreakdown(clickEventRepository.findBrowserBreakdown(shortUrlId)))
            .operatingSystemBreakdown(mapBreakdown(clickEventRepository.findOperatingSystemBreakdown(shortUrlId)))
            .deviceBreakdown(mapBreakdown(clickEventRepository.findDeviceBreakdown(shortUrlId)))
            .countryBreakdown(mapBreakdown(clickEventRepository.findCountryBreakdown(shortUrlId)))
            .build();
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(AppUser user) {
        return DashboardSummaryResponse.builder()
            .totalUrls(shortUrlRepository.countByOwnerId(user.getId()))
            .activeUrls(shortUrlRepository.countActiveByOwnerId(user.getId()))
            .expiredUrls(shortUrlRepository.countExpiredByOwnerId(user.getId()))
            .totalClicks(shortUrlRepository.totalClicksByOwnerId(user.getId()))
            .clickTrend(clickEventRepository.findOwnerDailyClicks(user.getId()).stream()
                .map(item -> DailyClickPoint.builder().day(item.getDay()).count(item.getCount()).build())
                .toList())
            .topUrls(mapBreakdown(clickEventRepository.findTopPerformingUrls(user.getId())))
            .build();
    }

    private List<MetricBreakdown> mapBreakdown(List<ClickEventRepository.MetricBreakdownProjection> source) {
        return source.stream().map(item -> MetricBreakdown.builder().label(item.getLabel()).count(item.getCount()).build()).toList();
    }
}
