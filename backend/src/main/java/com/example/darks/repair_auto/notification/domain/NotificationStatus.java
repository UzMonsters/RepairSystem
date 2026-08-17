package com.example.darks.repair_auto.notification.domain;

public enum NotificationStatus {
    PENDING,
    PROCESSING,
    RETRY_SCHEDULED,
    DELIVERED,
    FAILED,
    SKIPPED,
    DEAD;

    public NotificationStatus externalStatus() {
        return switch (this) {
            case PROCESSING, RETRY_SCHEDULED -> PENDING;
            case DEAD -> FAILED;
            default -> this;
        };
    }
}
