package com.example.darks.repair_auto.customer.api.dto;

import com.example.darks.repair_auto.customer.domain.CustomerRegistrationSource;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.OffsetDateTime;

public record CustomerSummaryResponse(
        Long id,
        String fullName,
        String phone,
        boolean phoneVerified,
        String email,
        boolean emailVerified,
        LanguageCode preferredLanguage,
        CustomerRegistrationSource registrationSource,
        boolean active,
        boolean telegramLinked,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
