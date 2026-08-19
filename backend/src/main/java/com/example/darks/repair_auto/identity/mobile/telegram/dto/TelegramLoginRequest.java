package com.example.darks.repair_auto.identity.mobile.telegram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Telegram OIDC login request containing the signed ID token")
public record TelegramLoginRequest(
        @NotBlank(message = "idToken is required")
        @Size(max = 4096, message = "idToken must not exceed 4096 characters")
        @Schema(
                description = "Signed Telegram OIDC ID token returned by official Telegram Login SDK",
                example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
        String idToken
) {
}
