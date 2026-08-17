package com.example.darks.repair_auto.notification.api.dto;

import com.example.darks.repair_auto.notification.domain.NotificationChannel;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import java.time.OffsetDateTime;

public record NotificationSummaryResponse(
        Long id,
        NotificationType type,
        Recipient recipient,
        String title,
        String message,
        RepairRequestRef repairRequest,
        NotificationChannel channel,
        NotificationStatus deliveryStatus,
        OffsetDateTime createdAt) {

    public record Recipient(NotificationRecipientType type, Long id, String name) {
    }

    public record RepairRequestRef(Long id, String number) {
    }
}
