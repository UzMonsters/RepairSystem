package com.example.darks.repair_auto.user.dto;

import jakarta.validation.constraints.NotNull;

public record UserActivationRequest(
        @NotNull Boolean active,
        String reason
) {
}
