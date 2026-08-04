package com.example.darks.repair_auto.identity.api.dto;

import com.example.darks.repair_auto.identity.domain.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleChangeRequest(
        @NotNull UserRole role
) {
}
