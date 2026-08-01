package com.example.darks.repair_auto.user.dto;

import com.example.darks.repair_auto.user.domain.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleChangeRequest(
        @NotNull UserRole role
) {
}
