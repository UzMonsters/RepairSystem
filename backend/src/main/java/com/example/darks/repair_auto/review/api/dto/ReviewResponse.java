package com.example.darks.repair_auto.review.api.dto;

import com.example.darks.repair_auto.review.domain.ReviewSource;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.OffsetDateTime;

public record ReviewResponse(
        Long reviewId,
        Long repairRequestId,
        String requestNumber,
        int rating,
        String comment,
        ReviewSource source,
        LanguageCode submittedLanguage,
        OffsetDateTime submittedAt,
        Long customerId,
        String customerName,
        Long technicianId,
        String technicianName,
        ReviewCategoryResponse category) {
}
