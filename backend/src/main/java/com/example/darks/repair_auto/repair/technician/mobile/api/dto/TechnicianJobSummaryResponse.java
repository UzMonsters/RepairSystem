package com.example.darks.repair_auto.repair.technician.mobile.api.dto;

import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Summary representation of an assigned repair job for Technician list view")
public record TechnicianJobSummaryResponse(
        @Schema(description = "Repair request ID", example = "42")
        Long requestId,

        @Schema(description = "Repair assignment ID", example = "19")
        Long assignmentId,

        @Schema(description = "Unique request number", example = "REQ-2026-000042")
        String requestNumber,

        @Schema(description = "Repair request status", example = "IN_PROGRESS")
        RepairRequestStatus requestStatus,

        @Schema(description = "Localized request status label", example = "Ta'mirlash jarayonida")
        String requestStatusLabel,

        @Schema(description = "Assignment status", example = "ACCEPTED")
        AssignmentStatus assignmentStatus,

        @Schema(description = "Localized assignment status label", example = "Qabul qilingan")
        String assignmentStatusLabel,

        @Schema(description = "Repair category summary")
        CategorySummary category,

        @Schema(description = "Customer summary")
        CustomerSummary customer,

        @Schema(description = "Physical address or landmark", example = "Chilanzar 9, Tashkent")
        String address,

        @Schema(description = "Scheduled visit timestamp if set", example = "2026-08-19T09:00:00Z")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        OffsetDateTime scheduledVisitAt,

        @Schema(description = "Assignment timestamp", example = "2026-08-18T10:00:00Z")
        OffsetDateTime assignedAt
) {
    public record CategorySummary(
            @Schema(description = "Category ID", example = "4")
            Long id,

            @Schema(description = "Localized category name", example = "Konditsioner")
            String name
    ) {
    }

    public record CustomerSummary(
            @Schema(description = "Customer full name", example = "Ali Valiyev")
            String fullName
    ) {
    }
}
