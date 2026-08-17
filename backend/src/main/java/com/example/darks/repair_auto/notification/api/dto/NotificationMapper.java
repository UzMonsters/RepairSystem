package com.example.darks.repair_auto.notification.api.dto;

import com.example.darks.repair_auto.notification.application.NotificationTemplateService;
import com.example.darks.repair_auto.notification.domain.NotificationDeliveryAttempt;
import com.example.darks.repair_auto.notification.domain.NotificationOutbox;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.OffsetDateTime;
import java.util.List;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationSummaryResponse summary(
            NotificationOutbox notification,
            String recipientName,
            LanguageCode language,
            NotificationTemplateService templateService) {
        var rendered = templateService.render(
                notification.getNotificationType(),
                notification.getRecipientType(),
                notification.getPayloadJson(),
                notification.getPayloadVersion(),
                language);
        return new NotificationSummaryResponse(
                notification.getId(),
                notification.getNotificationType(),
                new NotificationSummaryResponse.Recipient(
                        notification.getRecipientType(),
                        notification.getRecipientId(),
                        recipientName),
                rendered.title(),
                rendered.message(),
                repairRequest(notification),
                notification.getChannel(),
                notification.getStatus().externalStatus(),
                notification.getCreatedAt());
    }

    public static NotificationDeliveryResponse delivery(
            NotificationOutbox notification,
            List<NotificationDeliveryAttempt> attempts) {
        return new NotificationDeliveryResponse(
                notification.getId(),
                notification.getId(),
                notification.getChannel(),
                notification.getStatus().externalStatus(),
                notification.getLanguage(),
                notification.getAttemptCount(),
                lastAttemptAt(attempts),
                notification.getDeliveredAt(),
                nextRetryAt(notification),
                failure(notification),
                notification.getRenderedTitle(),
                notification.getRenderedMessage(),
                notification.getSkippedAt(),
                notification.getDeadAt(),
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

    private static NotificationSummaryResponse.RepairRequestRef repairRequest(NotificationOutbox notification) {
        if (notification.getRepairRequest() == null) {
            return null;
        }
        return new NotificationSummaryResponse.RepairRequestRef(
                notification.getRepairRequest().getId(),
                notification.getRepairRequest().getRequestNumber());
    }

    private static NotificationDeliveryResponse.Failure failure(NotificationOutbox notification) {
        if (notification.getLastFailureCategory() == null) {
            return null;
        }
        return new NotificationDeliveryResponse.Failure(
                notification.getLastFailureCategory(),
                notification.getLastFailureCategory());
    }

    private static OffsetDateTime lastAttemptAt(List<NotificationDeliveryAttempt> attempts) {
        return attempts.isEmpty() ? null : attempts.get(0).getFinishedAt();
    }

    private static OffsetDateTime nextRetryAt(NotificationOutbox notification) {
        if (notification.getStatus() == NotificationStatus.PENDING
                || notification.getStatus() == NotificationStatus.RETRY_SCHEDULED) {
            return notification.getNextAttemptAt();
        }
        return null;
    }
}
