package com.opsflow.analytics;

import com.opsflow.shared.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<AnalyticsService.DashboardMetricsDTO>> getDashboardMetrics() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getDashboardMetrics()));
    }
}
