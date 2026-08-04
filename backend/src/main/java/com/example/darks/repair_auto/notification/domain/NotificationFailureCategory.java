package com.example.darks.repair_auto.notification.domain;

public final class NotificationFailureCategory {

    public static final String RECIPIENT_UNAVAILABLE = "RECIPIENT_UNAVAILABLE";
    public static final String TELEGRAM_TRANSIENT_FAILURE = "TELEGRAM_TRANSIENT_FAILURE";
    public static final String TELEGRAM_PERMANENT_FAILURE = "TELEGRAM_PERMANENT_FAILURE";
    public static final String PROCESSING_LEASE_EXPIRED = "PROCESSING_LEASE_EXPIRED";
    public static final String UNSUPPORTED_PAYLOAD_VERSION = "UNSUPPORTED_PAYLOAD_VERSION";
    public static final String TEMPLATE_RENDER_FAILED = "TEMPLATE_RENDER_FAILED";
    public static final String MAX_ATTEMPTS_EXHAUSTED = "MAX_ATTEMPTS_EXHAUSTED";
    public static final String MANUAL_RETRY = "MANUAL_RETRY";

    private NotificationFailureCategory() {
    }
}
