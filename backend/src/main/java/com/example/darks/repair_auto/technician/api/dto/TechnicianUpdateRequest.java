package com.example.darks.repair_auto.technician.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;

public record TechnicianUpdateRequest(
        @NotBlank @Size(max = 160) String fullName,
        @NotBlank @Size(max = 64) String phone,
        @Size(max = 254) String email,
        @Size(max = 120) String specialization,
        @Size(max = 1000) String notes,
        @NotNull Integer maximumConcurrentRequests,
        @NotNull LanguageCode preferredLanguage) {
    public TechnicianUpdateRequest(
            String fullName,
            String phone,
            String specialization,
            String notes,
            Integer maximumConcurrentRequests,
            LanguageCode preferredLanguage) {
        this(fullName, phone, null, specialization, notes, maximumConcurrentRequests, preferredLanguage);
    }
}
