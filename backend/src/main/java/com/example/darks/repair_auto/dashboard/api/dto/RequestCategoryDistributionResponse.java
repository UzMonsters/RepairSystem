package com.example.darks.repair_auto.dashboard.api.dto;

import com.example.darks.repair_auto.dashboard.domain.DashboardPeriod;
import java.util.List;

public record RequestCategoryDistributionResponse(
        DashboardPeriod period,
        long total,
        List<RequestCategoryDistributionItemResponse> items,
        RequestCategoryOtherResponse other) {
}
