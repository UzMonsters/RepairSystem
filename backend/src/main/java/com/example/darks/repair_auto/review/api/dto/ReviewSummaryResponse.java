package com.example.darks.repair_auto.review.api.dto;

import java.math.BigDecimal;

public record ReviewSummaryResponse(
        long totalReviews,
        BigDecimal averageRating,
        long rating1Count,
        long rating2Count,
        long rating3Count,
        long rating4Count,
        long rating5Count,
        long reviewsWithComment) {
}
