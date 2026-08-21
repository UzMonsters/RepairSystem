package com.example.darks.repair_auto.telegram.conversation;

import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TelegramChatCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramChatCleanupService.class);

    public void deleteQuietly(TelegramBotClient botClient, Long chatId, Long messageId, String botContext) {
        if (botClient == null || chatId == null || messageId == null) {
            return;
        }
        try {
            botClient.deleteMessage(chatId, messageId);
        } catch (RuntimeException exception) {
            logFailure("deleteMessage", chatId, messageId, botContext, exception);
        }
    }

    public void deleteAllQuietly(
            TelegramBotClient botClient,
            Long chatId,
            Collection<Long> messageIds,
            String botContext) {
        if (messageIds == null) {
            return;
        }
        for (Long messageId : messageIds) {
            deleteQuietly(botClient, chatId, messageId, botContext);
        }
    }

    public void removeKeyboardQuietly(TelegramBotClient botClient, Long chatId, Long messageId, String botContext) {
        if (botClient == null || chatId == null || messageId == null) {
            return;
        }
        try {
            botClient.editMessageReplyMarkup(chatId, messageId, null);
        } catch (RuntimeException exception) {
            logFailure("editMessageReplyMarkup", chatId, messageId, botContext, exception);
        }
    }

    public void editPromptQuietly(
            TelegramBotClient botClient,
            Long chatId,
            Long messageId,
            String text,
            String replyMarkupJson,
            String botContext) {
        if (botClient == null || chatId == null || messageId == null) {
            return;
        }
        try {
            botClient.editMessageText(chatId, messageId, text, replyMarkupJson);
        } catch (RuntimeException exception) {
            logFailure("editMessageText", chatId, messageId, botContext, exception);
        }
    }

    public void answerCallbackQuietly(TelegramBotClient botClient, String callbackQueryId, String botContext) {
        if (botClient == null || callbackQueryId == null || callbackQueryId.isBlank()) {
            return;
        }
        try {
            botClient.answerCallbackQuery(callbackQueryId);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Telegram callback acknowledgement failed operation={} botContext={} exceptionCategory={}",
                    "answerCallbackQuery",
                    botContext,
                    exception.getClass().getSimpleName());
        }
    }

    private void logFailure(
            String operation,
            Long chatId,
            Long messageId,
            String botContext,
            RuntimeException exception) {
        LOGGER.warn(
                "Telegram cleanup failed operation={} botContext={} chatId={} messageId={} exceptionCategory={}",
                operation,
                botContext,
                chatId,
                messageId,
                exception.getClass().getSimpleName());
    }
}
