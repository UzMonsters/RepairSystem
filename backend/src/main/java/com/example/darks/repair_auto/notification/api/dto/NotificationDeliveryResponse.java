package com.example.darks.repair_auto.notification.api.dto;

import com.example.darks.repair_auto.notification.domain.NotificationChannel;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record NotificationDeliveryResponse(
        Long id,
        Long notificationId,
        NotificationChannel channel,
        NotificationStatus status,
        String language,
        int attemptCount,
        OffsetDateTime lastAttemptAt,
        OffsetDateTime deliveredAt,
        OffsetDateTime nextRetryAt,
        Failure failure,
        String renderedTitle,
        String renderedMessage,
        OffsetDateTime skippedAt,
        OffsetDateTime deadAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<NotificationAttemptResponse> attempts) {

    public record Failure(String category, String message) {
    }
}
