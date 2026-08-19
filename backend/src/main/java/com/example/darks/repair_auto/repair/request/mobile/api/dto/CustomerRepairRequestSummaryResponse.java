package com.example.darks.repair_auto.repair.request.mobile.api.dto;

import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Customer repair request summary item for list views")
public record CustomerRepairRequestSummaryResponse(
        @Schema(description = "Repair request ID", example = "42")
        Long id,

        @Schema(description = "Unique human-readable request number", example = "REQ-2026-000042")
        String requestNumber,

        @Schema(description = "Technical request status", example = "IN_PROGRESS")
        RepairRequestStatus status,

        @Schema(description = "Localized status display label", example = "Ta'mirlash jarayonida")
        String statusLabel,

        @Schema(description = "Category details")
        CategorySummary category,

        @Schema(description = "Customer problem description", example = "Air conditioner is not cooling")
        String description,

        @Schema(description = "Scheduled visit timestamp if planned", example = "2026-08-18T14:00:00Z")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        OffsetDateTime scheduledVisitAt,

        @Schema(description = "Creation timestamp", example = "2026-08-17T09:15:00Z")
        OffsetDateTime createdAt
) {
    public record CategorySummary(
            @Schema(description = "Category ID", example = "4")
            Long id,

            @Schema(description = "Localized category name", example = "Konditsioner")
            String name
    ) {
    }
}
