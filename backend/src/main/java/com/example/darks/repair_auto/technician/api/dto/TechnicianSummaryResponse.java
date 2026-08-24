package com.example.darks.repair_auto.technician.api.dto;

import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.OffsetDateTime;

public record TechnicianSummaryResponse(
        Long id,
        String fullName,
        String phone,
        boolean phoneVerified,
        String email,
        boolean emailVerified,
        String specialization,
        int maximumConcurrentRequests,
        LanguageCode preferredLanguage,
        boolean active,
        boolean telegramLinked,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
