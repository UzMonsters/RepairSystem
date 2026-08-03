package com.example.darks.repair_auto.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email
) {
}
