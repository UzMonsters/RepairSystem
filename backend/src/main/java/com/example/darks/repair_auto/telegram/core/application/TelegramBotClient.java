package com.example.darks.repair_auto.telegram.core.application;

import java.io.InputStream;
import java.util.List;

public interface TelegramBotClient {

    Long sendMessage(Long chatId, String text, String replyMarkupJson);

    void answerCallback(String callbackQueryId, String text);

    default void answerCallbackQuery(String callbackQueryId) {
        answerCallback(callbackQueryId, "");
    }

    void deleteMessage(Long chatId, Long messageId);

    default void deleteMessages(Long chatId, List<Long> messageIds) {
        if (messageIds == null) {
            return;
        }
        for (Long messageId : messageIds) {
            deleteMessage(chatId, messageId);
        }
    }

    void editMessageText(Long chatId, Long messageId, String text, String replyMarkupJson);

    void editMessageReplyMarkup(Long chatId, Long messageId, String replyMarkupJson);

    TelegramFileMetadata getFile(String fileId);

    InputStream downloadFile(String filePath, long maxSizeBytes);

    void sendPhoto(Long chatId, String filename, byte[] photoBytes, String caption);

    void sendMediaGroup(Long chatId, List<TelegramMediaPhoto> photos);

    void sendLocation(Long chatId, double latitude, double longitude);
}
