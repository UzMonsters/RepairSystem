package com.example.darks.repair_auto.settings.api.dto;

import com.example.darks.repair_auto.settings.domain.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SystemSettingsUpdateRequest(
        @NotBlank String timezone,
        @NotNull Language defaultLanguage
) {
}
