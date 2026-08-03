package com.example.darks.repair_auto.repair.request.api.dto;

import com.example.darks.repair_auto.identity.domain.UserRole;

public record RepairRequestUserSummary(
        Long id,
        String fullName,
        String email,
        UserRole role) {
}
