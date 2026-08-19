package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.domain.NotificationFailureCategory;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class NotificationDeliveryService {

    private final NotificationRecipientResolver recipientResolver;
    private final NotificationTemplateService templateService;
    private final TelegramBotClient customerTelegramBotClient;
    private final TelegramBotClient technicianTelegramBotClient;
    private final TechnicianAssignmentNotificationService technicianAssignmentNotificationService;

    public NotificationDeliveryService(
            NotificationRecipientResolver recipientResolver,
            NotificationTemplateService templateService,
            @Qualifier("customerTelegramBotClient") TelegramBotClient customerTelegramBotClient,
            @Qualifier("technicianTelegramBotClient") TelegramBotClient technicianTelegramBotClient,
            TechnicianAssignmentNotificationService technicianAssignmentNotificationService) {
        this.recipientResolver = recipientResolver;
        this.templateService = templateService;
        this.customerTelegramBotClient = customerTelegramBotClient;
        this.technicianTelegramBotClient = technicianTelegramBotClient;
        this.technicianAssignmentNotificationService = technicianAssignmentNotificationService;
    }

    public NotificationDeliveryResult deliver(ClaimedNotification notification) {
        var recipient = recipientResolver.resolve(notification);
        if (recipient.isEmpty()) {
            return NotificationDeliveryResult.unavailable(NotificationFailureCategory.RECIPIENT_UNAVAILABLE);
        }
        if (notification.notificationType() == com.example.darks.repair_auto.notification.domain.NotificationType.TECHNICIAN_ASSIGNED
                && notification.recipientType() == NotificationRecipientType.TECHNICIAN) {
            return technicianAssignmentNotificationService.deliverAssignment(notification, recipient.get());
        }
        String text = templateService.renderTelegramText(new NotificationTemplateService.RenderedNotification(
                recipient.get().language(),
                notification.renderedTitle(),
                notification.renderedMessage()));
        try {
            botClient(notification.recipientType()).sendMessage(recipient.get().chatId(), text, null);
            return NotificationDeliveryResult.delivered();
        } catch (TelegramApiException exception) {
            if (isPermanent(exception)) {
                return NotificationDeliveryResult.permanentFailure(
                        NotificationFailureCategory.TELEGRAM_PERMANENT_FAILURE);
            }
            return NotificationDeliveryResult.transientFailure(
                    NotificationFailureCategory.TELEGRAM_TRANSIENT_FAILURE);
        }
    }

    private TelegramBotClient botClient(NotificationRecipientType recipientType) {
        return recipientType == NotificationRecipientType.TECHNICIAN
                ? technicianTelegramBotClient
                : customerTelegramBotClient;
    }

    private boolean isPermanent(TelegramApiException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
        if (exception.getCause() != null && exception.getCause().getMessage() != null) {
            message = message + " " + exception.getCause().getMessage().toLowerCase();
        }
        return message.contains("blocked")
                || message.contains("chat not found")
                || message.contains("bad request")
                || message.contains("forbidden")
                || message.contains("permanent");
    }
}
