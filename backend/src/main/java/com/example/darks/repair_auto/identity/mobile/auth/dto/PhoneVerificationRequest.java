package com.example.darks.repair_auto.identity.mobile.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneVerificationRequest(
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "validation.phone.invalid")
        String phone
) {
}
