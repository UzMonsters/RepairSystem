package com.example.darks.repair_auto.customer.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerActivationRequest(
        @NotNull Boolean active,
        @Size(max = 500) String reason) {
}
