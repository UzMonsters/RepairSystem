package com.example.darks.repair_auto.technician.api.dto;

import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.OffsetDateTime;

public record TechnicianDetailResponse(
        Long id,
        String fullName,
        String phone,
        String specialization,
        String notes,
        int maximumConcurrentRequests,
        LanguageCode preferredLanguage,
        boolean active,
        boolean telegramLinked,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
