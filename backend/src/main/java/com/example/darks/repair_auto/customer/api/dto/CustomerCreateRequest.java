package com.example.darks.repair_auto.customer.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;

public record CustomerCreateRequest(
        @NotBlank @Size(max = 160) String fullName,
        @NotBlank @Size(max = 64) String phone,
        LanguageCode preferredLanguage) {
}
