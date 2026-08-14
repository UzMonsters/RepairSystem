package com.example.darks.repair_auto.settings.api.dto;

import com.example.darks.repair_auto.settings.domain.Language;

public record SystemSettingsResponse(
        String timezone,
        Language defaultLanguage
) {
    public static SystemSettingsResponse defaults() {
        return new SystemSettingsResponse("Asia/Tashkent", Language.UZ);
    }
}
