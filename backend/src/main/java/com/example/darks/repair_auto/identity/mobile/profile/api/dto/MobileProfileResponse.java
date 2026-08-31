package com.example.darks.repair_auto.identity.mobile.profile.api.dto;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.profile.api.dto.AvatarResponse;
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

        @Schema(description = "Whether the phone number has been verified by SMS OTP", example = "true")
        boolean phoneVerified,

        @Schema(description = "Optional profile email", example = "user@example.com")
        String email,

        @Schema(description = "Whether the profile email has been verified by RepairAuto email code", example = "true")
        boolean emailVerified,

        @Schema(description = "Preferred interface language code (uz, ru, en)", example = "uz")
        String preferredLanguage,

        @Schema(description = "Whether a Telegram account is linked", example = "true")
        boolean telegramLinked,

        @Schema(description = "Profile avatar metadata")
        AvatarResponse avatar,

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
        return forCustomer(id, fullName, phone, false, null, false, preferredLanguage, telegramLinked, null);
    }

    public static MobileProfileResponse forCustomer(
            Long id,
            String fullName,
            String phone,
            boolean phoneVerified,
            String email,
            boolean emailVerified,
            String preferredLanguage,
            boolean telegramLinked) {
        return forCustomer(id, fullName, phone, phoneVerified, email, emailVerified, preferredLanguage, telegramLinked, null);
    }

    public static MobileProfileResponse forCustomer(
            Long id,
            String fullName,
            String phone,
            boolean phoneVerified,
            String email,
            boolean emailVerified,
            String preferredLanguage,
            boolean telegramLinked,
            AvatarResponse avatar) {
        return new MobileProfileResponse(
                ActorType.CUSTOMER,
                id,
                fullName,
                phone,
                phoneVerified,
                email,
                emailVerified,
                preferredLanguage,
                telegramLinked,
                avatar,
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
        return forTechnician(
                id,
                fullName,
                phone,
                false,
                null,
                false,
                preferredLanguage,
                telegramLinked,
                specialization,
                maxActiveJobs,
                active,
                null);
    }

    public static MobileProfileResponse forTechnician(
            Long id,
            String fullName,
            String phone,
            boolean phoneVerified,
            String email,
            boolean emailVerified,
            String preferredLanguage,
            boolean telegramLinked,
            String specialization,
            int maxActiveJobs,
            boolean active) {
        return forTechnician(
                id,
                fullName,
                phone,
                phoneVerified,
                email,
                emailVerified,
                preferredLanguage,
                telegramLinked,
                specialization,
                maxActiveJobs,
                active,
                null);
    }

    public static MobileProfileResponse forTechnician(
            Long id,
            String fullName,
            String phone,
            boolean phoneVerified,
            String email,
            boolean emailVerified,
            String preferredLanguage,
            boolean telegramLinked,
            String specialization,
            int maxActiveJobs,
            boolean active,
            AvatarResponse avatar) {
        return new MobileProfileResponse(
                ActorType.TECHNICIAN,
                id,
                fullName,
                phone,
                phoneVerified,
                email,
                emailVerified,
                preferredLanguage,
                telegramLinked,
                avatar,
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
