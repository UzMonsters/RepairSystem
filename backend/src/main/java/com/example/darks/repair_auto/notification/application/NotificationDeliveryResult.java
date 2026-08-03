package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.domain.NotificationAttemptOutcome;

public record NotificationDeliveryResult(
        NotificationAttemptOutcome outcome,
        String failureCategory,
        String providerMessageId) {

    public static NotificationDeliveryResult delivered() {
        return new NotificationDeliveryResult(NotificationAttemptOutcome.DELIVERED, null, null);
    }

    public static NotificationDeliveryResult unavailable(String failureCategory) {
        return new NotificationDeliveryResult(
                NotificationAttemptOutcome.RECIPIENT_UNAVAILABLE,
                failureCategory,
                null);
    }

    public static NotificationDeliveryResult transientFailure(String failureCategory) {
        return new NotificationDeliveryResult(
                NotificationAttemptOutcome.TRANSIENT_FAILURE,
                failureCategory,
                null);
    }

    public static NotificationDeliveryResult permanentFailure(String failureCategory) {
        return new NotificationDeliveryResult(
                NotificationAttemptOutcome.PERMANENT_FAILURE,
                failureCategory,
                null);
    }
}
