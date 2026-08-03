package com.example.darks.repair_auto.identity.api.dto;

import jakarta.validation.constraints.NotNull;

public record UserActivationRequest(
        @NotNull Boolean active,
        String reason
) {
}
