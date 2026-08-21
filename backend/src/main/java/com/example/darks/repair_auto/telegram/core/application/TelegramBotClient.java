package com.example.darks.repair_auto.telegram.core.application;

import java.io.InputStream;
import java.util.List;

public interface TelegramBotClient {

    void sendMessage(Long chatId, String text, String replyMarkupJson);

    void answerCallback(String callbackQueryId, String text);

    TelegramFileMetadata getFile(String fileId);

    InputStream downloadFile(String filePath, long maxSizeBytes);

    void sendPhoto(Long chatId, String filename, byte[] photoBytes, String caption);

    void sendMediaGroup(Long chatId, List<TelegramMediaPhoto> photos);

    void sendLocation(Long chatId, double latitude, double longitude);
}
