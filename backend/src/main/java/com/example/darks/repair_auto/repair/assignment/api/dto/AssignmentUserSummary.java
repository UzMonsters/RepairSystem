package com.example.darks.repair_auto.repair.assignment.api.dto;

import com.example.darks.repair_auto.identity.domain.UserRole;

public record AssignmentUserSummary(
        Long id,
        String fullName,
        String email,
        UserRole role) {
}
