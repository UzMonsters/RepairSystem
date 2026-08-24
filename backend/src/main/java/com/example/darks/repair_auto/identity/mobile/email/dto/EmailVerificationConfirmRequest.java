package com.example.darks.repair_auto.identity.mobile.email.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EmailVerificationConfirmRequest(
        @NotNull UUID challengeId,
        @NotBlank String code
) {
}
