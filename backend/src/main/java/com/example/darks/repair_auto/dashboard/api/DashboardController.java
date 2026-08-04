package com.example.darks.repair_auto.dashboard.api;

import com.example.darks.repair_auto.dashboard.api.dto.DashboardOverviewResponse;
import com.example.darks.repair_auto.dashboard.api.dto.RequestCategoryDistributionResponse;
import com.example.darks.repair_auto.dashboard.api.dto.RequestStatusDistributionResponse;
import com.example.darks.repair_auto.dashboard.api.dto.RequestTrendResponse;
import com.example.darks.repair_auto.dashboard.api.dto.ReviewDashboardResponse;
import com.example.darks.repair_auto.dashboard.api.dto.TechnicianDashboardResponse;
import com.example.darks.repair_auto.dashboard.application.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/v1/dashboard/overview")
    @Operation(summary = "Get dashboard overview metrics")
    public DashboardOverviewResponse overview() {
        return dashboardService.overview();
    }

    @GetMapping("/api/v1/dashboard/request-trends")
    @Operation(summary = "Get request trend buckets")
    public RequestTrendResponse requestTrends(@RequestParam(required = false) String period) {
        return dashboardService.requestTrends(period);
    }

    @GetMapping("/api/v1/dashboard/requests-by-status")
    @Operation(summary = "Get current request status distribution")
    public RequestStatusDistributionResponse requestsByStatus() {
        return dashboardService.requestsByStatus();
    }

    @GetMapping("/api/v1/dashboard/requests-by-category")
    @Operation(summary = "Get request category distribution")
    public RequestCategoryDistributionResponse requestsByCategory(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer limit) {
        return dashboardService.requestsByCategory(period, limit);
    }

    @GetMapping("/api/v1/dashboard/technicians")
    @Operation(summary = "Get technician operational summary")
    public TechnicianDashboardResponse technicians() {
        return dashboardService.technicians();
    }

    @GetMapping("/api/v1/dashboard/reviews")
    @Operation(summary = "Get customer review dashboard snapshot")
    public ReviewDashboardResponse reviews() {
        return dashboardService.reviews();
    }
}
