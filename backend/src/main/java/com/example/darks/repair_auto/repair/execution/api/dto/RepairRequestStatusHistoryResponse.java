package com.example.darks.repair_auto.repair.execution.api.dto;

import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestUserSummary;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import java.time.OffsetDateTime;

public record RepairRequestStatusHistoryResponse(
        Long id,
        Long repairRequestId,
        RepairRequestStatus fromStatus,
        RepairRequestStatus toStatus,
        String reason,
        RepairRequestUserSummary changedBy,
        OffsetDateTime changedAt) {
}
