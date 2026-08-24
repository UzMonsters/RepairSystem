package com.example.darks.repair_auto.identity.mobile.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PhoneVerificationConfirmRequest(
        @NotNull
        UUID challengeId,

        @NotBlank
        @Size(min = 4, max = 8)
        String code
) {
}
