package com.example.darks.repair_auto.telegram.conversation;

import com.example.darks.repair_auto.telegram.core.api.TelegramUpdatePayload;
import org.springframework.stereotype.Component;

@Component
public class TelegramInputClassifier {

    public TelegramInputType classify(TelegramUpdatePayload update) {
        if (update == null) {
            return TelegramInputType.OTHER;
        }
        if (update.callbackQuery() != null) {
            return TelegramInputType.CALLBACK;
        }
        String text = update.text();
        if (text != null && text.trim().startsWith("/")) {
            return TelegramInputType.COMMAND;
        }
        if (text != null && !text.isBlank()) {
            return TelegramInputType.TEXT;
        }
        if (update.contact() != null) {
            return TelegramInputType.CONTACT;
        }
        if (update.location() != null) {
            return TelegramInputType.LOCATION;
        }
        if (!update.photo().isEmpty()) {
            return TelegramInputType.PHOTO;
        }
        return TelegramInputType.OTHER;
    }
}
