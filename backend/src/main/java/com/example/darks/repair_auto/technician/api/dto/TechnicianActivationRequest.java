package com.example.darks.repair_auto.technician.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TechnicianActivationRequest(
        @NotNull Boolean active,
        @Size(max = 500) String reason) {
}
