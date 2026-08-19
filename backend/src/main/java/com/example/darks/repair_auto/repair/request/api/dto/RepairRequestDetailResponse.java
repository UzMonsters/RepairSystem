package com.example.darks.repair_auto.repair.request.api.dto;

import com.example.darks.repair_auto.repair.assignment.api.dto.CurrentAssignmentSummary;
import com.example.darks.repair_auto.repair.execution.api.dto.RepairExecutionSummary;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestSource;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Repair request detail response")
public record RepairRequestDetailResponse(
        Long id,
        String requestNumber,
        RepairRequestStatus status,
        RepairRequestPriority priority,
        RepairRequestSource source,
        String description,
        RequestLocationResponse location,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        OffsetDateTime customerPreferredVisitAt,
        String internalNote,
        RepairRequestCustomerSummary customer,
        RepairRequestCategorySummary category,
        CurrentAssignmentSummary currentAssignment,
        RepairExecutionSummary execution,
        RepairRequestUserSummary createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public RepairRequestDetailResponse(
            Long id,
            String requestNumber,
            RepairRequestStatus status,
            RepairRequestPriority priority,
            RepairRequestSource source,
            String description,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            OffsetDateTime customerPreferredVisitAt,
            String internalNote,
            RepairRequestCustomerSummary customer,
            RepairRequestCategorySummary category,
            CurrentAssignmentSummary currentAssignment,
            RepairExecutionSummary execution,
            RepairRequestUserSummary createdBy,
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
                latitude,
                longitude,
                customerPreferredVisitAt,
                internalNote,
                customer,
                category,
                currentAssignment,
                execution,
                createdBy,
                createdAt,
                updatedAt);
    }
}
