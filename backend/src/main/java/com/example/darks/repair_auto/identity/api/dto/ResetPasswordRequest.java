package com.example.darks.repair_auto.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        String newPassword,

        @NotBlank
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        String confirmPassword
) {
}
