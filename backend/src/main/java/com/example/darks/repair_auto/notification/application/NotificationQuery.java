package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import java.time.OffsetDateTime;

public record NotificationQuery(
        NotificationStatus status,
        NotificationType notificationType,
        NotificationRecipientType recipientType,
        Long repairRequestId,
        OffsetDateTime createdFrom,
        OffsetDateTime createdTo) {
}
