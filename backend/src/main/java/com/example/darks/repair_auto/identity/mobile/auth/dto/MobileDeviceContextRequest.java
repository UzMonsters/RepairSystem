package com.example.darks.repair_auto.identity.mobile.auth.dto;

import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MobileDeviceContextRequest(
        @NotNull
        @Schema(example = "ANDROID")
        PushPlatform platform,

        @Size(max = 128)
        @Schema(example = "install-8f436b")
        String deviceId,

        @Size(max = 160)
        @Schema(example = "Pixel 8")
        String deviceName,

        @Size(max = 64)
        @Schema(example = "1.4.0")
        String appVersion
) {
}
