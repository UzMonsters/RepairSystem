package com.example.darks.repair_auto.telegram.core.infrastructure;

import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import com.example.darks.repair_auto.telegram.core.application.TelegramMediaPhoto;
import java.io.InputStream;
import java.util.List;

class NoopTelegramBotClient implements TelegramBotClient {

    @Override
    public Long sendMessage(Long chatId, String text, String replyMarkupJson) {
        return null;
    }

    @Override
    public void answerCallback(String callbackQueryId, String text) {
    }

    @Override
    public void deleteMessage(Long chatId, Long messageId) {
    }

    @Override
    public void editMessageText(Long chatId, Long messageId, String text, String replyMarkupJson) {
    }

    @Override
    public void editMessageReplyMarkup(Long chatId, Long messageId, String replyMarkupJson) {
    }

    @Override
    public TelegramFileMetadata getFile(String fileId) {
        throw new TelegramApiException("Telegram is disabled.");
    }

    @Override
    public InputStream downloadFile(String filePath, long maxSizeBytes) {
        throw new TelegramApiException("Telegram is disabled.");
    }

    @Override
    public void sendPhoto(Long chatId, String filename, byte[] photoBytes, String caption) {
    }

    @Override
    public void sendMediaGroup(Long chatId, List<TelegramMediaPhoto> photos) {
    }

    @Override
    public void sendLocation(Long chatId, double latitude, double longitude) {
    }
}
