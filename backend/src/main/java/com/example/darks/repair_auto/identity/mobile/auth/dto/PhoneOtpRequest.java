package com.example.darks.repair_auto.identity.mobile.auth.dto;

import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PhoneOtpRequest(
        @NotNull PushClientType clientType,
        @NotBlank String phone
) {
}
