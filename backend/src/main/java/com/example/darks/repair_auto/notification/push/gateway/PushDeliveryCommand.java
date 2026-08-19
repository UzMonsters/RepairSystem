package com.example.darks.repair_auto.notification.push.gateway;

import com.example.darks.repair_auto.notification.push.domain.PushEndpoint;
import java.util.Map;
import java.util.Objects;

public record PushDeliveryCommand(
        PushEndpoint endpoint,
        String title,
        String body,
        String notificationType,
        Long notificationId,
        Long repairRequestId,
        String requestNumber,
        String route,
        Map<String, String> additionalData
) {
    public PushDeliveryCommand {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body must not be blank");
        }
        if (notificationType == null || notificationType.isBlank()) {
            throw new IllegalArgumentException("notificationType must not be blank");
        }
    }
}
