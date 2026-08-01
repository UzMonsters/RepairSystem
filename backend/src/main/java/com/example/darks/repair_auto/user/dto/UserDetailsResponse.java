package com.example.darks.repair_auto.user.dto;

import com.example.darks.repair_auto.user.domain.UserRole;
import java.time.OffsetDateTime;

public record UserDetailsResponse(
        Long id,
        String fullName,
        String email,
        UserRole role,
        boolean active,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt
) {
}
