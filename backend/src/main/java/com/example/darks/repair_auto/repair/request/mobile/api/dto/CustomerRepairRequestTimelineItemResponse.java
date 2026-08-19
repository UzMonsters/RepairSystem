package com.example.darks.repair_auto.repair.request.mobile.api.dto;

import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Status timeline transition item for Customer view")
public record CustomerRepairRequestTimelineItemResponse(
        @Schema(description = "Target status of this transition", example = "IN_PROGRESS")
        RepairRequestStatus status,

        @Schema(description = "Localized status label", example = "Ta'mirlash boshlandi")
        String label,

        @Schema(description = "Timestamp when the status transition occurred", example = "2026-08-18T10:00:00Z")
        OffsetDateTime occurredAt
) {
}
