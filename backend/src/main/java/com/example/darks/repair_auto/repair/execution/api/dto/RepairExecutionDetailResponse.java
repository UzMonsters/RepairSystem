package com.example.darks.repair_auto.repair.execution.api.dto;

import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestUserSummary;
import java.time.OffsetDateTime;

public record RepairExecutionDetailResponse(
        Long id,
        Long repairRequestId,
        OffsetDateTime startedAt,
        RepairRequestUserSummary startedBy,
        String diagnosis,
        OffsetDateTime diagnosisUpdatedAt,
        RepairRequestUserSummary diagnosisUpdatedBy,
        String waitingReason,
        OffsetDateTime waitingSince,
        String workPerformed,
        String completionNote,
        OffsetDateTime completedAt,
        RepairRequestUserSummary completedBy,
        String cancellationReason,
        OffsetDateTime cancelledAt,
        RepairRequestUserSummary cancelledBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
