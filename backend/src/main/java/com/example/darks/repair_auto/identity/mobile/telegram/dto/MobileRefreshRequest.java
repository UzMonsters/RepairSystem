package com.example.darks.repair_auto.identity.mobile.telegram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to rotate mobile refresh token and receive a new token pair")
public record MobileRefreshRequest(
        @NotBlank
        @Size(max = 256)
        @Schema(description = "Opaque mobile refresh token", example = "dBjftJeZ4CVP-mB92K...")
        String refreshToken
) {
}
