package com.example.darks.repair_auto.customer.api.dto;

import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerUpdateRequest(
        @NotBlank @Size(max = 160) String fullName,
        @NotBlank @Size(max = 64) String phone,
        @NotNull LanguageCode preferredLanguage) {
}
