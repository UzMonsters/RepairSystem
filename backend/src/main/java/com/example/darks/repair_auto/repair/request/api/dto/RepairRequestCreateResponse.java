package com.example.darks.repair_auto.repair.request.api.dto;

import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestSource;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import java.time.OffsetDateTime;

public record RepairRequestCreateResponse(
        Long id,
        String requestNumber,
        RepairRequestStatus status,
        RepairRequestPriority priority,
        RepairRequestSource source,
        OffsetDateTime createdAt) {
}
