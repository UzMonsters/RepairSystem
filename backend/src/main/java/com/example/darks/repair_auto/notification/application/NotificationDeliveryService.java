package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.domain.NotificationFailureCategory;
import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import org.springframework.stereotype.Service;

@Service
public class NotificationDeliveryService {

    private final NotificationRecipientResolver recipientResolver;
    private final NotificationTemplateService templateService;
    private final TelegramBotClient telegramBotClient;

    public NotificationDeliveryService(
            NotificationRecipientResolver recipientResolver,
            NotificationTemplateService templateService,
            TelegramBotClient telegramBotClient) {
        this.recipientResolver = recipientResolver;
        this.templateService = templateService;
        this.telegramBotClient = telegramBotClient;
    }

    public NotificationDeliveryResult deliver(ClaimedNotification notification) {
        var recipient = recipientResolver.resolve(notification);
        if (recipient.isEmpty()) {
            return NotificationDeliveryResult.unavailable(NotificationFailureCategory.RECIPIENT_UNAVAILABLE);
        }
        String text;
        try {
            text = templateService.render(
                    notification.notificationType(),
                    notification.payloadJson(),
                    notification.payloadVersion(),
                    recipient.get().language());
        } catch (IllegalArgumentException exception) {
            return NotificationDeliveryResult.permanentFailure(NotificationFailureCategory.TEMPLATE_RENDER_FAILED);
        }
        try {
            telegramBotClient.sendMessage(recipient.get().chatId(), text, null);
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
