package com.example.darks.repair_auto.repair.assignment.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record AssignmentRequest(
        @NotNull Long technicianId,
        OffsetDateTime scheduledVisitAt) {
}
