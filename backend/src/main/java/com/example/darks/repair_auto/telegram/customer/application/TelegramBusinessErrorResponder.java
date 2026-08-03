package com.example.darks.repair_auto.telegram.customer.application;

import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.telegram.core.api.TelegramUpdatePayload;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.customer.infrastructure.TelegramCustomerSessionRepository;
import org.springframework.stereotype.Component;

@Component
public class TelegramBusinessErrorResponder {

    private final TelegramCustomerSessionRepository sessionRepository;
    private final TelegramMessages messages;
    private final TelegramBotClient botClient;

    public TelegramBusinessErrorResponder(
            TelegramCustomerSessionRepository sessionRepository,
            TelegramMessages messages,
            TelegramBotClient botClient) {
        this.sessionRepository = sessionRepository;
        this.messages = messages;
        this.botClient = botClient;
    }

    public void respond(TelegramUpdatePayload update, BusinessRuleException exception) {
        TelegramUpdatePayload.TelegramChat chat = update.chat();
        TelegramUpdatePayload.TelegramUser sender = update.sender();
        if (chat == null || sender == null || chat.id() == null || !"private".equals(chat.type())) {
            return;
        }
        LanguageCode language = sessionRepository.findByTelegramUserId(sender.id())
                .map(session -> session.getLanguage())
                .orElse(LanguageCode.UZ);
        botClient.sendMessage(chat.id(), messages.businessError(language, exception.code()), null);
    }
}
