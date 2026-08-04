package com.example.darks.repair_auto.telegram.core.infrastructure;

import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import java.io.InputStream;

class NoopTelegramBotClient implements TelegramBotClient {

    @Override
    public void sendMessage(Long chatId, String text, String replyMarkupJson) {
    }

    @Override
    public void answerCallback(String callbackQueryId, String text) {
    }

    @Override
    public TelegramFileMetadata getFile(String fileId) {
        throw new TelegramApiException("Telegram is disabled.");
    }

    @Override
    public InputStream downloadFile(String filePath, long maxSizeBytes) {
        throw new TelegramApiException("Telegram is disabled.");
    }
}
