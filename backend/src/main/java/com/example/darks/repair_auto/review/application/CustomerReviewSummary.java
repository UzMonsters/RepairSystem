package com.example.darks.repair_auto.review.application;

public record CustomerReviewSummary(
        Long reviewId,
        int rating,
        String comment) {
}
