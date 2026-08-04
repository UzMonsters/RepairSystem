package com.example.darks.repair_auto.dashboard.api.dto;

import com.example.darks.repair_auto.dashboard.domain.DashboardPeriod;
import java.time.LocalDate;
import java.util.List;

public record RequestTrendResponse(
        DashboardPeriod period,
        LocalDate fromDate,
        LocalDate toDate,
        List<RequestTrendBucketResponse> buckets) {
}
