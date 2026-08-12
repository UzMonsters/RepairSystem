package com.example.darks.repair_auto.telegram.core.application;

import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.telegram.core.api.TelegramUpdatePayload;
import com.example.darks.repair_auto.telegram.core.domain.TelegramUpdateRecord;
import com.example.darks.repair_auto.telegram.core.domain.TelegramUpdateStatus;
import com.example.darks.repair_auto.telegram.core.domain.TelegramUserContext;
import com.example.darks.repair_auto.telegram.core.domain.TelegramUserMode;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramUpdateRepository;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramUserContextRepository;
import com.example.darks.repair_auto.telegram.customer.application.TelegramBusinessErrorResponder;
import com.example.darks.repair_auto.telegram.customer.application.TelegramCustomerBotService;
import com.example.darks.repair_auto.telegram.technician.application.TelegramTechnicianBotService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TelegramWebhookService {

    private final TelegramUpdateRepository updateRepository;
    private final TelegramUserContextRepository contextRepository;
    private final TelegramCustomerBotService customerBotService;
    private final TelegramTechnicianBotService technicianBotService;
    private final TelegramBusinessErrorResponder businessErrorResponder;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Autowired
    public TelegramWebhookService(
            TelegramUpdateRepository updateRepository,
            TelegramUserContextRepository contextRepository,
            TelegramCustomerBotService customerBotService,
            TelegramTechnicianBotService technicianBotService,
            TelegramBusinessErrorResponder businessErrorResponder,
            PlatformTransactionManager transactionManager) {
        this(
                updateRepository,
                contextRepository,
                customerBotService,
                technicianBotService,
                businessErrorResponder,
                new ObjectMapper(),
                new TransactionTemplate(transactionManager),
                Clock.systemUTC());
    }

    TelegramWebhookService(
            TelegramUpdateRepository updateRepository,
            TelegramUserContextRepository contextRepository,
            TelegramCustomerBotService customerBotService,
            TelegramTechnicianBotService technicianBotService,
            TelegramBusinessErrorResponder businessErrorResponder,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            Clock clock) {
        this.updateRepository = updateRepository;
        this.contextRepository = contextRepository;
        this.customerBotService = customerBotService;
        this.technicianBotService = technicianBotService;
        this.businessErrorResponder = businessErrorResponder;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    public void process(String rawBody) {
        process(rawBody, null);
    }

    public void process(String rawBody, TelegramUserMode forcedMode) {
        TelegramUpdatePayload update = parse(rawBody);
        if (update.updateId() == null) {
            throw new BusinessRuleException("TELEGRAM_UPDATE_INVALID", "Telegram update is invalid.", 400);
        }
        if (!claim(update)) {
            return;
        }
        try {
            handle(update, forcedMode);
            markProcessed(update.updateId());
        } catch (BusinessRuleException exception) {
            try {
                respond(update, exception, forcedMode);
                markProcessed(update.updateId());
            } catch (TelegramApiException | org.springframework.dao.TransientDataAccessException deliveryException) {
                markReceived(update.updateId());
                throw deliveryException;
            }
        } catch (TelegramApiException | org.springframework.dao.TransientDataAccessException exception) {
            markFailed(update.updateId(), exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private void handle(TelegramUpdatePayload update, TelegramUserMode forcedMode) {
        TelegramUserMode mode = mode(update, forcedMode);
        if (mode == TelegramUserMode.TECHNICIAN) {
            technicianBotService.handle(update);
            return;
        }
        customerBotService.handle(update);
    }

    private void respond(TelegramUpdatePayload update, BusinessRuleException exception, TelegramUserMode forcedMode) {
        if (forcedMode == TelegramUserMode.TECHNICIAN
                || currentMode(update) == TelegramUserMode.TECHNICIAN
                || isTechnicianCommand(update)) {
            technicianBotService.respondBusinessError(update, exception);
            return;
        }
        businessErrorResponder.respond(update, exception);
    }

    private TelegramUserMode mode(TelegramUpdatePayload update, TelegramUserMode forcedMode) {
        TelegramUpdatePayload.TelegramChat chat = update.chat();
        TelegramUpdatePayload.TelegramUser sender = update.sender();
        if (chat == null || sender == null || chat.id() == null) {
            return TelegramUserMode.CUSTOMER;
        }
        TelegramUserMode mode = forcedMode == null ? requestedMode(update) : forcedMode;
        if (mode == TelegramUserMode.TECHNICIAN && (isTechnicianStart(update) || isTechnicianLanguageSelection(update))) {
            return TelegramUserMode.TECHNICIAN;
        }
        if (mode == TelegramUserMode.TECHNICIAN) {
            technicianBotService.requireSwitchAllowed(sender.id(), chat.id());
        } else if (mode == TelegramUserMode.CUSTOMER) {
            customerBotService.requireSwitchAllowed(sender.id(), chat.id());
        }
        if (mode == null) {
            mode = currentMode(update);
        }
        if (mode == null) {
            mode = TelegramUserMode.CUSTOMER;
        }
        TelegramUserMode finalMode = mode;
        TelegramUserContext context = contextRepository.findByTelegramUserId(sender.id())
                .orElseGet(() -> contextRepository.saveAndFlush(
                        new TelegramUserContext(sender.id(), chat.id(), finalMode, now())));
        context.switchMode(finalMode, chat.id(), now());
        return finalMode;
    }

    private TelegramUserMode requestedMode(TelegramUpdatePayload update) {
        String text = update.text();
        if (text != null && (text.equalsIgnoreCase("/technician") || text.startsWith("/start tech_"))) {
            return TelegramUserMode.TECHNICIAN;
        }
        String callbackData = update.callbackQuery() == null ? null : update.callbackQuery().data();
        if (callbackData != null && callbackData.startsWith("t")) {
            return TelegramUserMode.TECHNICIAN;
        }
        if (text != null && text.equalsIgnoreCase("/customer")) {
            return TelegramUserMode.CUSTOMER;
        }
        return null;
    }

    private TelegramUserMode currentMode(TelegramUpdatePayload update) {
        TelegramUpdatePayload.TelegramUser sender = update.sender();
        if (sender == null) {
            return null;
        }
        return contextRepository.findByTelegramUserId(sender.id())
                .map(TelegramUserContext::getActiveMode)
                .orElse(null);
    }

    private boolean isTechnicianCommand(TelegramUpdatePayload update) {
        String text = update.text();
        String callbackData = update.callbackQuery() == null ? null : update.callbackQuery().data();
        return (text != null && (text.equalsIgnoreCase("/technician") || text.startsWith("/start tech_")))
                || (callbackData != null && callbackData.startsWith("t"));
    }

    private boolean isTechnicianStart(TelegramUpdatePayload update) {
        String text = update.text();
        return text != null && text.startsWith("/start tech_");
    }

    private boolean isTechnicianLanguageSelection(TelegramUpdatePayload update) {
        String callbackData = update.callbackQuery() == null ? null : update.callbackQuery().data();
        return callbackData != null && callbackData.startsWith("tlang:");
    }

    private TelegramUpdatePayload parse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, TelegramUpdatePayload.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("TELEGRAM_UPDATE_INVALID", "Telegram update is invalid.", 400);
        }
    }

    private boolean claim(TelegramUpdatePayload update) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            updateRepository.insertReceivedIfAbsent(update.updateId(), update.updateType(), now());
            TelegramUpdateRecord record = updateRepository.findByTelegramUpdateIdForUpdate(update.updateId())
                    .orElseThrow(() -> new BusinessRuleException("TELEGRAM_UPDATE_INVALID", "Telegram update is invalid.", 400));
            if (record.isProcessed() || record.isProcessing()) {
                return false;
            }
            if (record.getStatus() == TelegramUpdateStatus.FAILED) {
                record.retry(now());
            } else {
                record.processing(now());
            }
            return true;
        }));
    }

    private void markProcessed(Long telegramUpdateId) {
        transactionTemplate.executeWithoutResult(status -> updateRepository.findByTelegramUpdateIdForUpdate(telegramUpdateId)
                .ifPresent(record -> record.processed(now())));
    }

    private void markReceived(Long telegramUpdateId) {
        transactionTemplate.executeWithoutResult(status -> updateRepository.findByTelegramUpdateIdForUpdate(telegramUpdateId)
                .ifPresent(record -> record.received(now())));
    }

    private void markFailed(Long telegramUpdateId, String failureCategory) {
        transactionTemplate.executeWithoutResult(status -> updateRepository.findByTelegramUpdateIdForUpdate(telegramUpdateId)
                .ifPresent(record -> record.failed(failureCategory, now())));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
