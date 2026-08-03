package com.example.darks.repair_auto.repair.request.application;

import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestSource;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import java.time.OffsetDateTime;

public record RepairRequestQuery(
        String search,
        String requestNumber,
        Long customerId,
        Long categoryId,
        RepairRequestStatus status,
        RepairRequestPriority priority,
        RepairRequestSource source,
        OffsetDateTime createdFrom,
        OffsetDateTime createdTo,
        OffsetDateTime preferredVisitFrom,
        OffsetDateTime preferredVisitTo) {
}
