package com.example.darks.repair_auto.identity.mobile.profile.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Self-service mobile profile patch request")
public record MobileProfilePatchRequest(
        @Size(max = 160, message = "fullName must not exceed 160 characters")
        @Schema(description = "Updated full name (editable by Customer only)", example = "Ali Valiyev")
        String fullName,

        @Schema(description = "Updated preferred language (UZ, RU, EN)", example = "uz")
        String preferredLanguage
) {
}
