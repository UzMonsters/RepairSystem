package com.example.darks.repair_auto.profile.api.dto;

import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.settings.domain.DateFormat;
import com.example.darks.repair_auto.settings.domain.Language;
import com.example.darks.repair_auto.settings.domain.Theme;
import com.example.darks.repair_auto.settings.domain.TimeFormat;
import java.time.OffsetDateTime;

public record ProfileResponse(
        Long id,
        String username,
        String fullName,
        String phone,
        UserRole role,
        boolean active,
        AvatarResponse avatar,
        Language language,
        DateFormat dateFormat,
        TimeFormat timeFormat,
        Theme theme,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
