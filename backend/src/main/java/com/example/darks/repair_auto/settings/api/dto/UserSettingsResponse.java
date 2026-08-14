package com.example.darks.repair_auto.settings.api.dto;

import com.example.darks.repair_auto.settings.domain.DateFormat;
import com.example.darks.repair_auto.settings.domain.Language;
import com.example.darks.repair_auto.settings.domain.Theme;
import com.example.darks.repair_auto.settings.domain.TimeFormat;

public record UserSettingsResponse(
        Language language,
        DateFormat dateFormat,
        TimeFormat timeFormat,
        Theme theme
) {
    public static UserSettingsResponse defaults() {
        return new UserSettingsResponse(
                Language.UZ,
                DateFormat.DD_MM_YYYY,
                TimeFormat.HOUR_24,
                Theme.SYSTEM
        );
    }
}
