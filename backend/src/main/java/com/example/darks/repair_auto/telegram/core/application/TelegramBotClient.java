package com.example.darks.repair_auto.telegram.core.application;

import java.io.InputStream;

public interface TelegramBotClient {

    void sendMessage(Long chatId, String text, String replyMarkupJson);

    void answerCallback(String callbackQueryId, String text);

    TelegramFileMetadata getFile(String fileId);

    InputStream downloadFile(String filePath, long maxSizeBytes);
}
