package com.example.darks.repair_auto.notification.domain;

public enum NotificationAttemptOutcome {
    DELIVERED,
    TRANSIENT_FAILURE,
    PERMANENT_FAILURE,
    RECIPIENT_UNAVAILABLE,
    LEASE_RECOVERED
}
