package com.example.darks.repair_auto.identity.mobile.telegram.dto;

import com.example.darks.repair_auto.identity.domain.ActorType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary of authenticated mobile actor")
public record MobileActorSummary(
        @Schema(description = "Actor type (CUSTOMER or TECHNICIAN)", example = "CUSTOMER")
        ActorType type,

        @Schema(description = "Internal actor ID", example = "42")
        Long id,

        @Schema(description = "Full name", example = "Ali Valiyev")
        String fullName,

        @Schema(description = "Phone number", example = "+998901234567")
        String phone,

        @Schema(description = "Preferred language code", example = "uz")
        String preferredLanguage
) {
}
