package com.example.darks.repair_auto.notification.push.gateway;

public enum PushDeliveryStatus {
    SUCCESS,
    PERMANENT_FAILURE,
    RETRYABLE_FAILURE,
    CONFIGURATION_FAILURE,
    INVALID_PAYLOAD
}
