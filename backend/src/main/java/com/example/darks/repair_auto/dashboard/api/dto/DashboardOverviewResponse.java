package com.example.darks.repair_auto.dashboard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DashboardOverviewResponse(
        OffsetDateTime generatedAt,
        LocalDate businessDate,
        long totalRequests,
        long newToday,
        long openRequests,
        long inProgress,
        long waitingForParts,
        long completedToday,
        long completedTotal,
        long cancelledTotal,
        long activeTechnicians,
        long techniciansWithActiveWork,
        long pendingAssignments,
        BigDecimal averageRating,
        long totalReviews) {
}
