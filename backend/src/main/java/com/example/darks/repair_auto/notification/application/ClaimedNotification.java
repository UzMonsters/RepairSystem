package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationType;

public record ClaimedNotification(
        Long notificationId,
        NotificationType notificationType,
        NotificationRecipientType recipientType,
        Long recipientId,
        String templateKey,
        String payloadJson,
        int payloadVersion) {
}
