package com.example.darks.repair_auto.repair.request.api.dto;

import com.example.darks.repair_auto.shared.i18n.LanguageCode;

public record RepairRequestCustomerSummary(
        Long id,
        String fullName,
        String phone,
        LanguageCode preferredLanguage,
        boolean active) {
}
