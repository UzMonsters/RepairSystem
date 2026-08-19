package com.example.darks.repair_auto.repair.technician.mobile.api.dto;

import com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Detailed representation of an assigned repair job for Technician field view")
public record TechnicianJobDetailResponse(
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

        @Schema(description = "Problem description", example = "Air conditioner is not cooling")
        String description,

        @Schema(description = "Customer contact information")
        CustomerInfo customer,

        @Schema(description = "Location information")
        LocationInfo location,

        @Schema(description = "Schedule information")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ScheduleInfo schedule,

        @Schema(description = "Repair execution details")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ExecutionInfo execution,

        @Schema(description = "Actions currently available to the authenticated Technician on this job")
        List<RepairAvailableAction> availableActions,

        @Schema(description = "Creation timestamp", example = "2026-08-18T09:00:00Z")
        OffsetDateTime createdAt,

        @Schema(description = "Last update timestamp", example = "2026-08-18T10:00:00Z")
        OffsetDateTime updatedAt
) {
    public record CategorySummary(
            @Schema(description = "Category ID", example = "4")
            Long id,

            @Schema(description = "Localized category name", example = "Konditsioner")
            String name
    ) {
    }

    public record CustomerInfo(
            @Schema(description = "Customer ID", example = "101")
            Long id,

            @Schema(description = "Customer full name", example = "Ali Valiyev")
            String fullName,

            @Schema(description = "Customer phone number", example = "+998901234567")
            String phone
    ) {
    }

    public record LocationInfo(
            @Schema(description = "Street address or landmark", example = "Chilanzar 9, Tashkent")
            String address,

            @Schema(description = "Latitude", example = "41.275412")
            BigDecimal latitude,

            @Schema(description = "Longitude", example = "69.204511")
            BigDecimal longitude,

            @Schema(description = "Location acquisition source method", example = "DEVICE_GPS")
            com.example.darks.repair_auto.repair.request.domain.RequestLocationSource source
    ) {
        public LocationInfo(String address, BigDecimal latitude, BigDecimal longitude) {
            this(address, latitude, longitude, null);
        }
    }

    public record ScheduleInfo(
            @Schema(description = "Scheduled visit timestamp", example = "2026-08-19T09:00:00Z")
            OffsetDateTime scheduledVisitAt
    ) {
    }

    public record ExecutionInfo(
            @Schema(description = "Technician diagnostic notes", example = "Capacitor failed")
            String diagnosis,

            @Schema(description = "Description of work performed", example = "Replaced capacitor")
            String workPerformed,

            @Schema(description = "Completion note", example = "Unit tested successfully")
            String completionNote,

            @Schema(description = "Reason why execution is waiting for parts", example = "Ordering capacitor")
            String waitingForPartsReason
    ) {
    }
}
