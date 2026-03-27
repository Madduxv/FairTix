package com.fairtix.analytics.api;

import com.fairtix.analytics.application.AnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides aggregate analytics for the admin dashboard.
 *
 * <p>Returns event counts, seat utilisation, hold metrics, and user breakdowns
 * in a single response to minimise round-trips.
 */
@Tag(name = "Analytics", description = "Admin dashboard analytics")
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Returns a comprehensive analytics snapshot for the admin dashboard.
     *
     * @return aggregate stats covering events, seats, holds, and users
     */
    @Operation(summary = "Get dashboard analytics",
            description = "Admin-only. Returns aggregate statistics for the admin dashboard.")
    @ApiResponse(responseCode = "200", description = "Analytics snapshot")
    @ApiResponse(responseCode = "403", description = "Not an admin")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    public AnalyticsResponse getDashboardAnalytics() {
        return analyticsService.getDashboardAnalytics();
    }
}
