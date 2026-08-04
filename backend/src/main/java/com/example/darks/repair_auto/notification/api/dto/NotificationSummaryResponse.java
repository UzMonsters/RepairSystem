package com.example.darks.repair_auto.notification.api.dto;

import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import java.time.OffsetDateTime;

public record NotificationSummaryResponse(
        Long id,
        String eventKey,
        NotificationType notificationType,
        NotificationRecipientType recipientType,
        Long recipientId,
        Long repairRequestId,
        String requestNumber,
        NotificationStatus status,
        int attemptCount,
        OffsetDateTime nextAttemptAt,
        OffsetDateTime deliveredAt,
        OffsetDateTime skippedAt,
        OffsetDateTime deadAt,
        String lastFailureCategory,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
