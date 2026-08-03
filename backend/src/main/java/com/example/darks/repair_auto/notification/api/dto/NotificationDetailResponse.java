package com.example.darks.repair_auto.notification.api.dto;

import com.example.darks.repair_auto.notification.domain.NotificationChannel;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import java.time.OffsetDateTime;
import java.util.List;

public record NotificationDetailResponse(
        Long id,
        String eventKey,
        NotificationType notificationType,
        NotificationChannel channel,
        NotificationRecipientType recipientType,
        Long recipientId,
        Long repairRequestId,
        String requestNumber,
        String templateKey,
        NotificationStatus status,
        int attemptCount,
        OffsetDateTime nextAttemptAt,
        OffsetDateTime deliveredAt,
        OffsetDateTime skippedAt,
        OffsetDateTime deadAt,
        String lastFailureCategory,
        OffsetDateTime lastFailureAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<NotificationAttemptResponse> attempts) {
}
