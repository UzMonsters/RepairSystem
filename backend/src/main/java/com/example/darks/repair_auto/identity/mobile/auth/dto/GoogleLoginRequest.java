package com.example.darks.repair_auto.identity.mobile.auth.dto;

import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GoogleLoginRequest(
        @NotNull PushClientType clientType,
        @NotBlank String idToken,
        @Valid MobileDeviceContextRequest device
) {

    public GoogleLoginRequest(PushClientType clientType, String idToken) {
        this(clientType, idToken, null);
    }
}
