package com.example.darks.repair_auto.dashboard.api.dto;

import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import java.math.BigDecimal;

public record RequestStatusDistributionItemResponse(
        RepairRequestStatus status,
        DashboardStatusLabelResponse label,
        long count,
        BigDecimal percentage) {
}
