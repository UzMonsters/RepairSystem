package com.example.darks.repair_auto.notification.push.gateway;

import java.util.Objects;

public record PushDeliveryResult(
        PushDeliveryStatus status,
        String firebaseMessageId,
        String errorCode,
        String errorMessage
) {
    public PushDeliveryResult {
        Objects.requireNonNull(status, "status must not be null");
    }

    public static PushDeliveryResult success(String messageId) {
        return new PushDeliveryResult(PushDeliveryStatus.SUCCESS, messageId, null, null);
    }

    public static PushDeliveryResult permanentFailure(String errorCode, String errorMessage) {
        return new PushDeliveryResult(PushDeliveryStatus.PERMANENT_FAILURE, null, errorCode, errorMessage);
    }

    public static PushDeliveryResult retryableFailure(String errorCode, String errorMessage) {
        return new PushDeliveryResult(PushDeliveryStatus.RETRYABLE_FAILURE, null, errorCode, errorMessage);
    }

    public static PushDeliveryResult configurationFailure(String errorCode, String errorMessage) {
        return new PushDeliveryResult(PushDeliveryStatus.CONFIGURATION_FAILURE, null, errorCode, errorMessage);
    }

    public static PushDeliveryResult invalidPayload(String errorCode, String errorMessage) {
        return new PushDeliveryResult(PushDeliveryStatus.INVALID_PAYLOAD, null, errorCode, errorMessage);
    }

    public boolean isSuccess() {
        return status == PushDeliveryStatus.SUCCESS;
    }

    public boolean shouldDisableEndpoint() {
        return status == PushDeliveryStatus.PERMANENT_FAILURE;
    }
}
