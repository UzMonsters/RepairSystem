package com.example.darks.repair_auto.catalog.category.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryActivationRequest(
        @NotNull Boolean active,
        @Size(max = 500) String reason) {
}
