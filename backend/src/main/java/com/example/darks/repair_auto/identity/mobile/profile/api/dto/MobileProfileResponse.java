package com.example.darks.repair_auto.identity.mobile.profile.api.dto;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mobile self-service actor profile response")
public record MobileProfileResponse(
        @Schema(description = "Actor type: CUSTOMER or TECHNICIAN", example = "CUSTOMER")
        ActorType actorType,

        @Schema(description = "Unique actor ID", example = "42")
        Long id,

        @Schema(description = "Full name", example = "Ali Valiyev")
        String fullName,

        @Schema(description = "Phone number in international E.164 format", example = "+998901234567")
        String phone,

        @Schema(description = "Preferred interface language code (uz, ru, en)", example = "uz")
        String preferredLanguage,

        @Schema(description = "Whether a Telegram account is linked", example = "true")
        boolean telegramLinked,

        @Schema(description = "Technician operational metadata (omitted for Customer)")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        TechnicianProfileMetadata technician
) {

    public static MobileProfileResponse forCustomer(
            Long id,
            String fullName,
            String phone,
            String preferredLanguage,
            boolean telegramLinked) {
        return new MobileProfileResponse(
                ActorType.CUSTOMER,
                id,
                fullName,
                phone,
                preferredLanguage,
                telegramLinked,
                null);
    }

    public static MobileProfileResponse forTechnician(
            Long id,
            String fullName,
            String phone,
            String preferredLanguage,
            boolean telegramLinked,
            String specialization,
            int maxActiveJobs,
            boolean active) {
        return new MobileProfileResponse(
                ActorType.TECHNICIAN,
                id,
                fullName,
                phone,
                preferredLanguage,
                telegramLinked,
                new TechnicianProfileMetadata(specialization, maxActiveJobs, active));
    }

    public record TechnicianProfileMetadata(
            @Schema(description = "Technician specialization", example = "Washing Machine Master")
            String specialization,

            @Schema(description = "Maximum concurrent active requests / capacity", example = "5")
            int maxActiveJobs,

            @Schema(description = "Whether technician account is active", example = "true")
            boolean active
    ) {
    }
}
