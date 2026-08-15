package com.example.darks.repair_auto.profile.api.dto;

import com.example.darks.repair_auto.settings.domain.DateFormat;
import com.example.darks.repair_auto.settings.domain.Language;
import com.example.darks.repair_auto.settings.domain.Theme;
import com.example.darks.repair_auto.settings.domain.TimeFormat;

public record UpdateProfileRequest(
        String fullName,
        String phone,
        Language language,
        DateFormat dateFormat,
        TimeFormat timeFormat,
        Theme theme
) {
}
