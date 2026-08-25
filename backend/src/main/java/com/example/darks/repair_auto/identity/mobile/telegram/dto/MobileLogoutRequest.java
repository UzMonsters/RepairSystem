package com.example.darks.repair_auto.identity.mobile.telegram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to log out a mobile session using the opaque refresh token")
public record MobileLogoutRequest(
        @NotBlank
        @Size(max = 256)
        @Schema(description = "Opaque mobile refresh token to revoke", example = "dBjftJeZ4CVP-mB92K...")
        String refreshToken
) {
}
