package com.example.darks.repair_auto.identity.api.dto;

import com.example.darks.repair_auto.identity.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        String password,
        @NotNull UserRole role,
        Boolean active
) {
}
