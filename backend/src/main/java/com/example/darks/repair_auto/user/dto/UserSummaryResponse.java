package com.example.darks.repair_auto.user.dto;

import com.example.darks.repair_auto.user.domain.UserRole;

public record UserSummaryResponse(
        Long id,
        String fullName,
        String email,
        UserRole role
) {
}
