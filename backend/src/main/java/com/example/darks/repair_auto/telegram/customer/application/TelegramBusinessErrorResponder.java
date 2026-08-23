package com.example.darks.repair_auto.telegram.customer.application;

import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.telegram.core.api.TelegramUpdatePayload;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSession;
import com.example.darks.repair_auto.telegram.customer.infrastructure.TelegramCustomerSessionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class TelegramBusinessErrorResponder {

    private final TelegramCustomerSessionRepository sessionRepository;
    private final TelegramMessages messages;
    private final TelegramBotClient botClient;

    public TelegramBusinessErrorResponder(
            TelegramCustomerSessionRepository sessionRepository,
            TelegramMessages messages,
            @Qualifier("customerTelegramBotClient") TelegramBotClient botClient) {
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
                .map(TelegramCustomerSession::getLanguage)
                .orElse(LanguageCode.UZ);
        botClient.sendMessage(chat.id(), messages.businessError(language, exception.code()), null);
    }

    public void respondCallback(TelegramUpdatePayload.TelegramCallbackQuery callback, BusinessRuleException exception) {
        if (callback == null || callback.id() == null) {
            return;
        }
        LanguageCode language = LanguageCode.UZ;
        if (callback.from() != null && callback.from().id() != null) {
            language = sessionRepository.findByTelegramUserId(callback.from().id())
                    .map(TelegramCustomerSession::getLanguage)
                    .orElse(LanguageCode.UZ);
        }
        String errorMsg = messages.businessError(language, exception.code());
        botClient.answerCallback(callback.id(), errorMsg, true);
        if (callback.message() != null && callback.message().chat() != null && callback.message().chat().id() != null) {
            botClient.sendMessage(callback.message().chat().id(), errorMsg, null);
        }
    }
}
