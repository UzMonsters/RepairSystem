package com.example.darks.repair_auto.dashboard.api.dto;

import java.math.BigDecimal;

public record ReviewDashboardResponse(
        long totalReviews,
        BigDecimal averageRating,
        long reviewsWithComment,
        ReviewRatingDistributionResponse distribution) {
}
