package com.example.darks.repair_auto.repair.request.mobile.api.dto;

import com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Customer repair request detailed view")
public record CustomerRepairRequestDetailResponse(
        @Schema(description = "Repair request ID", example = "42")
        Long id,

        @Schema(description = "Unique human-readable request number", example = "REQ-2026-000042")
        String requestNumber,

        @Schema(description = "Technical request status", example = "IN_PROGRESS")
        RepairRequestStatus status,

        @Schema(description = "Localized status display label", example = "Ta'mirlash jarayonida")
        String statusLabel,

        @Schema(description = "Category details")
        CategorySummary category,

        @Schema(description = "Problem description", example = "Air conditioner is not cooling")
        String description,

        @Schema(description = "Location information")
        LocationInfo location,

        @Schema(description = "Assigned technician information if assigned")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        TechnicianSummary technician,

        @Schema(description = "Schedule information")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ScheduleInfo schedule,

        @Schema(description = "Customer review details if submitted")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ReviewInfo review,

        @Schema(description = "Actions currently available to the authenticated Customer on this request")
        List<RepairAvailableAction> availableActions,

        @Schema(description = "Creation timestamp", example = "2026-08-17T09:15:00Z")
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

    public record TechnicianSummary(
            @Schema(description = "Technician ID", example = "17")
            Long id,

            @Schema(description = "Technician full name", example = "Aziz Karimov")
            String fullName,

            @Schema(description = "Technician phone number", example = "+998901112233")
            String phone,

            @Schema(description = "Technician specialization", example = "Cooling Master")
            String specialization
    ) {
    }

    public record ScheduleInfo(
            @Schema(description = "Scheduled technician visit timestamp", example = "2026-08-18T14:00:00Z")
            OffsetDateTime scheduledVisitAt
    ) {
    }

    public record ReviewInfo(
            @Schema(description = "Review identifier", example = "71")
            Long id,

            @Schema(description = "Rating score from 1 to 5", example = "5")
            int rating,

            @Schema(description = "Review feedback comment", example = "Service was fast and reliable.")
            String comment,

            @Schema(description = "Review submission timestamp", example = "2026-08-18T12:30:00Z")
            OffsetDateTime submittedAt
    ) {
    }
}
