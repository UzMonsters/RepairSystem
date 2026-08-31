package com.example.darks.repair_auto.technician.api.dto;

import com.example.darks.repair_auto.profile.api.dto.AvatarResponse;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.OffsetDateTime;

public record TechnicianDetailResponse(
        Long id,
        String fullName,
        String phone,
        boolean phoneVerified,
        String email,
        boolean emailVerified,
        String specialization,
        String notes,
        int maximumConcurrentRequests,
        LanguageCode preferredLanguage,
        boolean active,
        boolean telegramLinked,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        AvatarResponse avatar) {

    public TechnicianDetailResponse(
            Long id,
            String fullName,
            String phone,
            boolean phoneVerified,
            String email,
            boolean emailVerified,
            String specialization,
            String notes,
            int maximumConcurrentRequests,
            LanguageCode preferredLanguage,
            boolean active,
            boolean telegramLinked,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this(id, fullName, phone, phoneVerified, email, emailVerified, specialization, notes, maximumConcurrentRequests, preferredLanguage, active, telegramLinked, createdAt, updatedAt, null);
    }
}
