package com.example.darks.repair_auto.telegram.conversation;

import com.example.darks.repair_auto.telegram.core.api.TelegramUpdatePayload;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSession;
import com.example.darks.repair_auto.telegram.technician.domain.TelegramTechnicianSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramUnexpectedInputHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramUnexpectedInputHandler.class);

    private final TelegramInputClassifier classifier;
    private final TelegramConversationPolicy policy;
    private final TelegramChatCleanupService cleanupService;

    public TelegramUnexpectedInputHandler(
            TelegramInputClassifier classifier,
            TelegramConversationPolicy policy,
            TelegramChatCleanupService cleanupService) {
        this.classifier = classifier;
        this.policy = policy;
        this.cleanupService = cleanupService;
    }

    public boolean cleanupIfUnexpected(
            TelegramCustomerSession session,
            TelegramUpdatePayload update,
            TelegramBotClient botClient) {
        TelegramInputType inputType = classifier.classify(update);
        if (policy.isAllowed(session.getState(), inputType)) {
            return false;
        }
        cleanup(update, botClient, "customer", inputType, session.getState().name());
        return true;
    }

    public boolean cleanupIfUnexpected(
            TelegramTechnicianSession session,
            TelegramUpdatePayload update,
            TelegramBotClient botClient) {
        TelegramInputType inputType = classifier.classify(update);
        if (policy.isAllowed(session.getState(), inputType)) {
            return false;
        }
        cleanup(update, botClient, "technician", inputType, session.getState().name());
        return true;
    }

    private void cleanup(
            TelegramUpdatePayload update,
            TelegramBotClient botClient,
            String botContext,
            TelegramInputType inputType,
            String state) {
        Long chatId = update.chat() == null ? null : update.chat().id();
        Long messageId = update.message() == null ? null : update.message().messageId();
        if (update.callbackQuery() != null) {
            cleanupService.answerCallbackQuietly(botClient, update.callbackQuery().id(), botContext);
            messageId = update.callbackQuery().message() == null ? null : update.callbackQuery().message().messageId();
            cleanupService.removeKeyboardQuietly(botClient, chatId, messageId, botContext);
        } else {
            cleanupService.deleteQuietly(botClient, chatId, messageId, botContext);
        }
        LOGGER.info(
                "Telegram unexpected input handled botContext={} chatId={} messageId={} state={} inputType={}",
                botContext,
                chatId,
                messageId,
                state,
                inputType);
    }
}
