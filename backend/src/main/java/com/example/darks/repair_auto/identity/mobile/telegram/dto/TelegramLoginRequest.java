package com.example.darks.repair_auto.identity.mobile.telegram.dto;

import com.example.darks.repair_auto.identity.mobile.auth.dto.MobileDeviceContextRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Telegram OIDC login request containing the signed ID token")
public record TelegramLoginRequest(
        @NotBlank
        @Size(max = 4096)
        @Schema(
                description = "Signed Telegram OIDC ID token returned by official Telegram Login SDK",
                example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
        String idToken,

        @Valid
        MobileDeviceContextRequest device
) {
        public TelegramLoginRequest(String idToken) {
                this(idToken, null);
        }
}
