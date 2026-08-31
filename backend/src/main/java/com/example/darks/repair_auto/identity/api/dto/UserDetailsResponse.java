package com.example.darks.repair_auto.identity.api.dto;

import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.profile.api.dto.AvatarResponse;
import java.time.OffsetDateTime;

public record UserDetailsResponse(
        Long id,
        String fullName,
        String email,
        UserRole role,
        boolean active,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        AvatarResponse avatar
) {
    public UserDetailsResponse(
            Long id,
            String fullName,
            String email,
            UserRole role,
            boolean active,
            OffsetDateTime lastLoginAt,
            OffsetDateTime createdAt
    ) {
        this(id, fullName, email, role, active, lastLoginAt, createdAt, null);
    }
}
