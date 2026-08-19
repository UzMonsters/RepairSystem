package com.example.darks.repair_auto.notification.push.api.dto;

import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.notification.push.domain.PushFirebaseApp;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Push endpoint registration details")
public record PushEndpointResponse(
        @Schema(description = "Push endpoint registration ID", example = "51")
        Long id,

        @Schema(description = "Client type", example = "CUSTOMER_MOBILE")
        PushClientType clientType,

        @Schema(description = "Client platform", example = "ANDROID")
        PushPlatform platform,

        @Schema(description = "Firebase app key", example = "CUSTOMER_ANDROID")
        PushFirebaseApp firebaseAppKey,

        @Schema(description = "Whether push notifications are enabled for this installation", example = "true")
        boolean enabled,

        @Schema(description = "Timestamp when this installation was last registered/seen", example = "2026-08-18T10:45:00Z")
        OffsetDateTime lastSeenAt
) {
}
