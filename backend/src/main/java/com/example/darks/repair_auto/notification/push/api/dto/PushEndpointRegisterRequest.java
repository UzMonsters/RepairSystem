package com.example.darks.repair_auto.notification.push.api.dto;

import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.notification.push.domain.PushFirebaseApp;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to register or refresh an FCM push endpoint")
public record PushEndpointRegisterRequest(
        @NotBlank
        @Size(min = 1, max = 512)
        @Schema(description = "FCM registration token returned by the Firebase client SDK", example = "dK3j9F8s7lQ:APA91bE...fcm_token")
        String fcmRegistrationToken,

        @NotNull
        @Schema(description = "Client type registering this endpoint", example = "CUSTOMER_MOBILE")
        PushClientType clientType,

        @NotNull
        @Schema(description = "Operating platform of the client device", example = "ANDROID")
        PushPlatform platform,

        @NotNull
        @Schema(description = "Backend-recognized Firebase application identifier", example = "CUSTOMER_ANDROID")
        PushFirebaseApp firebaseAppKey,

        @Size(max = 64)
        @Schema(description = "Optional client application version", example = "1.0.0")
        String appVersion
) {
}
