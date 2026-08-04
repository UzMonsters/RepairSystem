package com.example.darks.repair_auto.repair.request.api.dto;

import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = false)
public record RepairRequestCreateRequest(
        @NotNull Long customerId,
        @NotNull Long categoryId,
        @Size(max = 2000) String description,
        @Size(max = 500) String address,
        BigDecimal latitude,
        BigDecimal longitude,
        RepairRequestPriority priority,
        OffsetDateTime customerPreferredVisitAt,
        @Size(max = 2000) String internalNote) {
}
