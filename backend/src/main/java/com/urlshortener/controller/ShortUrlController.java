package com.urlshortener.controller;

import com.urlshortener.dto.analytics.UrlAnalyticsResponse;
import com.urlshortener.dto.common.PagedResponse;
import com.urlshortener.dto.url.BulkCreateShortUrlRequest;
import com.urlshortener.dto.url.CreateShortUrlRequest;
import com.urlshortener.dto.url.ShortUrlResponse;
import com.urlshortener.dto.url.UpdateShortUrlRequest;
import com.urlshortener.security.UserPrincipal;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.CurrentUserService;
import com.urlshortener.service.ShortUrlService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class ShortUrlController {

    private final ShortUrlService shortUrlService;
    private final CurrentUserService currentUserService;
    private final AnalyticsService analyticsService;

    @PostMapping
    @Operation(summary = "Create a shortened URL")
    public ShortUrlResponse create(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody CreateShortUrlRequest request) {
        return shortUrlService.createShortUrl(currentUserService.requireUser(principal), request);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Create multiple shortened URLs")
    public List<ShortUrlResponse> bulkCreate(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody BulkCreateShortUrlRequest request) {
        return shortUrlService.createBulk(currentUserService.requireUser(principal), request);
    }

    @GetMapping
    @Operation(summary = "List URLs owned by the authenticated user")
    public PagedResponse<ShortUrlResponse> list(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "ALL") String status,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String direction
    ) {
        return shortUrlService.listUrls(currentUserService.requireUser(principal), page, size, search, status, sortBy, direction);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one owned URL")
    public ShortUrlResponse get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return shortUrlService.getOwnedUrl(currentUserService.requireUser(principal), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update metadata on one owned URL")
    public ShortUrlResponse update(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateShortUrlRequest request
    ) {
        return shortUrlService.updateOwnedUrl(currentUserService.requireUser(principal), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate an owned URL")
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        shortUrlService.deactivateOwnedUrl(currentUserService.requireUser(principal), id);
    }

    @GetMapping("/{id}/analytics")
    @Operation(summary = "Fetch analytics for one owned URL")
    public UrlAnalyticsResponse analytics(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return analyticsService.getUrlAnalytics(currentUserService.requireUser(principal), id);
    }
}
