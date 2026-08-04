package com.example.darks.repair_auto.repair.assignment.api.dto;

import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import java.time.OffsetDateTime;

public record AssignmentDetailResponse(
        Long id,
        Long repairRequestId,
        AssignmentTechnicianSummary technician,
        AssignmentStatus status,
        OffsetDateTime scheduledVisitAt,
        AssignmentUserSummary assignedBy,
        OffsetDateTime assignedAt,
        OffsetDateTime respondedAt,
        String rejectionReason,
        String closureReason,
        OffsetDateTime closedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
