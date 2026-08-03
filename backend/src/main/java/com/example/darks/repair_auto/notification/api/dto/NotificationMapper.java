package com.example.darks.repair_auto.notification.api.dto;

import com.example.darks.repair_auto.notification.domain.NotificationDeliveryAttempt;
import com.example.darks.repair_auto.notification.domain.NotificationOutbox;
import java.util.List;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationSummaryResponse summary(NotificationOutbox notification) {
        return new NotificationSummaryResponse(
                notification.getId(),
                notification.getEventKey(),
                notification.getNotificationType(),
                notification.getRecipientType(),
                notification.getRecipientId(),
                requestId(notification),
                requestNumber(notification),
                notification.getStatus(),
                notification.getAttemptCount(),
                notification.getNextAttemptAt(),
                notification.getDeliveredAt(),
                notification.getSkippedAt(),
                notification.getDeadAt(),
                notification.getLastFailureCategory(),
                notification.getCreatedAt(),
                notification.getUpdatedAt());
    }

    public static NotificationDetailResponse detail(
            NotificationOutbox notification,
            List<NotificationDeliveryAttempt> attempts) {
        return new NotificationDetailResponse(
                notification.getId(),
                notification.getEventKey(),
                notification.getNotificationType(),
                notification.getChannel(),
                notification.getRecipientType(),
                notification.getRecipientId(),
                requestId(notification),
                requestNumber(notification),
                notification.getTemplateKey(),
                notification.getStatus(),
                notification.getAttemptCount(),
                notification.getNextAttemptAt(),
                notification.getDeliveredAt(),
                notification.getSkippedAt(),
                notification.getDeadAt(),
                notification.getLastFailureCategory(),
                notification.getLastFailureAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt(),
                attempts.stream().map(NotificationMapper::attempt).toList());
    }

    private static NotificationAttemptResponse attempt(NotificationDeliveryAttempt attempt) {
        return new NotificationAttemptResponse(
                attempt.getAttemptNumber(),
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                attempt.getOutcome(),
                attempt.getFailureCategory());
    }

    private static Long requestId(NotificationOutbox notification) {
        return notification.getRepairRequest() == null ? null : notification.getRepairRequest().getId();
    }

    private static String requestNumber(NotificationOutbox notification) {
        return notification.getRepairRequest() == null ? null : notification.getRepairRequest().getRequestNumber();
    }
}
