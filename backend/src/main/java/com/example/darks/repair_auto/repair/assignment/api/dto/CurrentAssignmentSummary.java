package com.example.darks.repair_auto.repair.assignment.api.dto;

import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import java.time.OffsetDateTime;

public record CurrentAssignmentSummary(
        Long id,
        Long repairRequestId,
        AssignmentTechnicianSummary technician,
        AssignmentStatus status,
        OffsetDateTime scheduledVisitAt,
        OffsetDateTime assignedAt,
        OffsetDateTime respondedAt) {
}
