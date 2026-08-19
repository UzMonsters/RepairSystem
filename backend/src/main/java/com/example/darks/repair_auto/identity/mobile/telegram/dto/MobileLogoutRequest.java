package com.example.darks.repair_auto.identity.mobile.telegram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to log out a mobile session using the opaque refresh token")
public record MobileLogoutRequest(
        @NotBlank(message = "refreshToken is required")
        @Size(max = 256, message = "refreshToken must not exceed 256 characters")
        @Schema(description = "Opaque mobile refresh token to revoke", example = "dBjftJeZ4CVP-mB92K...")
        String refreshToken
) {
}
