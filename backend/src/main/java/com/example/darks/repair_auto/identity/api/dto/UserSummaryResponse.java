package com.example.darks.repair_auto.identity.api.dto;

import com.example.darks.repair_auto.identity.domain.UserRole;

public record UserSummaryResponse(
        Long id,
        String fullName,
        String email,
        UserRole role
) {
}
