package com.example.darks.repair_auto.repair.request.mobile.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Customer repair review response")
public record CustomerReviewResponse(
        @Schema(description = "Review ID", example = "71")
        Long id,

        @Schema(description = "Rating score from 1 to 5", example = "5")
        int rating,

        @Schema(description = "Review feedback comment", example = "Service was fast and reliable.")
        String comment,

        @Schema(description = "Review submission timestamp", example = "2026-08-18T12:30:00Z")
        OffsetDateTime submittedAt
) {
}
