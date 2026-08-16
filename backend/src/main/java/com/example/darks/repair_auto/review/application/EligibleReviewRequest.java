package com.example.darks.repair_auto.review.application;

public record EligibleReviewRequest(
        Long requestId,
        String requestNumber,
        String categoryName,
        String categoryNameEn,
        String categoryNameRu,
        String categoryNameUz) {
}
