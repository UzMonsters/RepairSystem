package com.example.darks.repair_auto.identity.api.dto;

import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.profile.api.dto.AvatarResponse;

public record UserSummaryResponse(
        Long id,
        String fullName,
        String email,
        UserRole role,
        AvatarResponse avatar
) {
    public UserSummaryResponse(Long id, String fullName, String email, UserRole role) {
        this(id, fullName, email, role, null);
    }
}
