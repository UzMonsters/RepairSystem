package com.example.darks.repair_auto.repair.request.api.dto;

import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = false)
public record RepairRequestUpdateRequest(
        @NotNull Long customerId,
        @NotNull Long categoryId,
        @Size(max = 2000) String description,
        @Valid RequestLocationRequest location,
        @Size(max = 500) String address,
        BigDecimal latitude,
        BigDecimal longitude,
        @NotNull RepairRequestPriority priority,
        OffsetDateTime customerPreferredVisitAt,
        @Size(max = 2000) String internalNote) {

    public RepairRequestUpdateRequest(
            Long customerId,
            Long categoryId,
            String description,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            RepairRequestPriority priority,
            OffsetDateTime customerPreferredVisitAt,
            String internalNote) {
        this(
                customerId,
                categoryId,
                description,
                null,
                address,
                latitude,
                longitude,
                priority,
                customerPreferredVisitAt,
                internalNote);
    }

    public RequestLocationRequest resolvedLocation() {
        if (location != null) {
            return location;
        }
        if (address != null || latitude != null || longitude != null) {
            return new RequestLocationRequest(latitude, longitude, address, null);
        }
        return null;
    }
}
