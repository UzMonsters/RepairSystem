package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.domain.NotificationAttemptOutcome;
import java.time.OffsetDateTime;

public record NotificationDeliveryResult(
        NotificationAttemptOutcome outcome,
        String failureCategory,
        String providerMessageId,
        OffsetDateTime nextAttemptAt) {

    public NotificationDeliveryResult(
            NotificationAttemptOutcome outcome,
            String failureCategory,
            String providerMessageId) {
        this(outcome, failureCategory, providerMessageId, null);
    }

    public static NotificationDeliveryResult delivered() {
        return new NotificationDeliveryResult(NotificationAttemptOutcome.DELIVERED, null, null, null);
    }

    public static NotificationDeliveryResult delivered(String providerMessageId) {
        return new NotificationDeliveryResult(NotificationAttemptOutcome.DELIVERED, null, providerMessageId, null);
    }

    public static NotificationDeliveryResult unavailable(String failureCategory) {
        return new NotificationDeliveryResult(
                NotificationAttemptOutcome.RECIPIENT_UNAVAILABLE,
                failureCategory,
                null,
                null);
    }

    public static NotificationDeliveryResult transientFailure(String failureCategory) {
        return new NotificationDeliveryResult(
                NotificationAttemptOutcome.TRANSIENT_FAILURE,
                failureCategory,
                null,
                null);
    }

    public static NotificationDeliveryResult transientFailure(String failureCategory, OffsetDateTime nextAttemptAt) {
        return new NotificationDeliveryResult(
                NotificationAttemptOutcome.TRANSIENT_FAILURE,
                failureCategory,
                null,
                nextAttemptAt);
    }

    public static NotificationDeliveryResult permanentFailure(String failureCategory) {
        return new NotificationDeliveryResult(
                NotificationAttemptOutcome.PERMANENT_FAILURE,
                failureCategory,
                null,
                null);
    }
}
