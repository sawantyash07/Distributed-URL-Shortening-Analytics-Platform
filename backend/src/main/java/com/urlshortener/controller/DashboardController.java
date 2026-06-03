package com.urlshortener.controller;

import com.urlshortener.dto.analytics.DashboardSummaryResponse;
import com.urlshortener.security.UserPrincipal;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUserService;

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard metrics and charts")
    public DashboardSummaryResponse summary(@AuthenticationPrincipal UserPrincipal principal) {
        return analyticsService.getDashboardSummary(currentUserService.requireUser(principal));
    }
}
