package com.example.darks.repair_auto.identity.mobile.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PhoneOtpVerifyRequest(
        @NotNull UUID challengeId,
        @NotBlank String code,
        @Valid MobileDeviceContextRequest device
) {
}
