package com.example.darks.repair_auto.repair.request.api.dto;

import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestSource;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Repair request summary response for list views")
public record RepairRequestSummaryResponse(
        Long id,
        String requestNumber,
        RepairRequestStatus status,
        RepairRequestPriority priority,
        RepairRequestSource source,
        String description,
        RequestLocationResponse location,
        String address,
        OffsetDateTime customerPreferredVisitAt,
        String customerFullName,
        RepairRequestCategorySummary category,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public RepairRequestSummaryResponse(
            Long id,
            String requestNumber,
            RepairRequestStatus status,
            RepairRequestPriority priority,
            RepairRequestSource source,
            String description,
            String address,
            OffsetDateTime customerPreferredVisitAt,
            String customerFullName,
            RepairRequestCategorySummary category,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this(
                id,
                requestNumber,
                status,
                priority,
                source,
                description,
                null,
                address,
                customerPreferredVisitAt,
                customerFullName,
                category,
                createdAt,
                updatedAt);
    }
}
