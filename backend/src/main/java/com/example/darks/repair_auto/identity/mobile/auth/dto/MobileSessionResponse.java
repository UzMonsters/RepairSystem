package com.example.darks.repair_auto.identity.mobile.auth.dto;

import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MobileSessionResponse(
        UUID id,
        PushClientType clientType,
        MobileAuthProvider authenticationProvider,
        PushPlatform platform,
        String deviceId,
        String deviceName,
        String appVersion,
        OffsetDateTime createdAt,
        OffsetDateTime lastSeenAt,
        OffsetDateTime expiresAt,
        boolean revoked
) {
}
