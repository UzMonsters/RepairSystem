package com.example.darks.repair_auto.user.dto;

import com.example.darks.repair_auto.user.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull UserRole role,
        Boolean active
) {
}
