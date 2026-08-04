package com.example.darks.repair_auto.repair.assignment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record ReassignmentRequest(
        @NotNull Long technicianId,
        OffsetDateTime scheduledVisitAt,
        @NotBlank @Size(max = 500) String reason) {
}
