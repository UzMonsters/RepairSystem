package com.example.darks.repair_auto.identity.mobile.telegram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mobile authentication response containing RepairAuto access and refresh tokens")
public record MobileAuthResponse(
        @Schema(description = "Token type", example = "Bearer")
        String tokenType,

        @Schema(description = "RepairAuto access token (JWT)")
        String accessToken,

        @Schema(description = "Opaque RepairAuto refresh token")
        String refreshToken,

        @Schema(description = "Access token expiration in seconds", example = "900")
        long expiresIn,

        @Schema(description = "Refresh token expiration in seconds", example = "2592000")
        long refreshExpiresIn,

        @Schema(description = "Authenticated actor summary")
        MobileActorSummary actor
) {
}
