package com.example.darks.repair_auto.customer.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;

public record CustomerCreateRequest(
        @NotBlank @Size(max = 160) String fullName,
        @Size(max = 64) String phone,
        @Size(max = 254) String email,
        LanguageCode preferredLanguage) {
    public CustomerCreateRequest(String fullName, String phone, LanguageCode preferredLanguage) {
        this(fullName, phone, null, preferredLanguage);
    }
}
