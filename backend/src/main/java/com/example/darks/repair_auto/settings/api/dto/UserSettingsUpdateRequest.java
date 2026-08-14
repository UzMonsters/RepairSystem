package com.example.darks.repair_auto.settings.api.dto;

import com.example.darks.repair_auto.settings.domain.DateFormat;
import com.example.darks.repair_auto.settings.domain.Language;
import com.example.darks.repair_auto.settings.domain.Theme;
import com.example.darks.repair_auto.settings.domain.TimeFormat;
import jakarta.validation.constraints.NotNull;

public record UserSettingsUpdateRequest(
        @NotNull Language language,
        @NotNull DateFormat dateFormat,
        @NotNull TimeFormat timeFormat,
        @NotNull Theme theme
) {
}
