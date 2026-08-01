package com.example.darks.repair_auto.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email
) {
}
