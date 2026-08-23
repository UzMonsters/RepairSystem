package com.example.darks.repair_auto.telegram.core.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TelegramScreenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramScreenService.class);

    public Long sendOrEdit(
            TelegramBotClient client,
            Long chatId,
            Long messageIdToEdit,
            String text,
            String replyMarkupJson) {
        if (messageIdToEdit != null) {
            try {
                return client.editMessage(chatId, messageIdToEdit, text, replyMarkupJson);
            } catch (TelegramApiException exception) {
                LOGGER.debug(
                        "Telegram editMessage failed for chatId={} messageId={}, falling back to sendMessage: {}",
                        chatId,
                        messageIdToEdit,
                        exception.getMessage());
            }
        }
        return client.sendMessage(chatId, text, replyMarkupJson);
    }
}
