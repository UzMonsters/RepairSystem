package com.example.darks.repair_auto.repair.request.mobile.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Customer repair review submission request")
public record CustomerReviewCreateRequest(
        @NotNull(message = "validation.review.rating.required")
        @Min(value = 1, message = "validation.review.rating.min")
        @Max(value = 5, message = "validation.review.rating.max")
        @Schema(description = "Rating score from 1 to 5", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer rating,

        @Size(max = 1000, message = "validation.review.comment.max")
        @Schema(description = "Optional feedback comment", example = "Service was fast and reliable.", maxLength = 1000)
        String comment
) {
}
