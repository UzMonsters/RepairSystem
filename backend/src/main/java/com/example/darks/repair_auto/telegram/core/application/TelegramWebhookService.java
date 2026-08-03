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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramWebhookService {

    private final TelegramUpdateRepository updateRepository;
    private final TelegramUserContextRepository contextRepository;
    private final TelegramCustomerBotService customerBotService;
    private final TelegramTechnicianBotService technicianBotService;
    private final TelegramBusinessErrorResponder businessErrorResponder;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public TelegramWebhookService(
            TelegramUpdateRepository updateRepository,
            TelegramUserContextRepository contextRepository,
            TelegramCustomerBotService customerBotService,
            TelegramTechnicianBotService technicianBotService,
            TelegramBusinessErrorResponder businessErrorResponder) {
        this(
                updateRepository,
                contextRepository,
                customerBotService,
                technicianBotService,
                businessErrorResponder,
                new ObjectMapper(),
                Clock.systemUTC());
    }

    TelegramWebhookService(
            TelegramUpdateRepository updateRepository,
            TelegramUserContextRepository contextRepository,
            TelegramCustomerBotService customerBotService,
            TelegramTechnicianBotService technicianBotService,
            TelegramBusinessErrorResponder businessErrorResponder,
            ObjectMapper objectMapper,
            Clock clock) {
        this.updateRepository = updateRepository;
        this.contextRepository = contextRepository;
        this.customerBotService = customerBotService;
        this.technicianBotService = technicianBotService;
        this.businessErrorResponder = businessErrorResponder;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = {
            TelegramApiException.class,
            org.springframework.dao.TransientDataAccessException.class
    })
    public void process(String rawBody) {
        TelegramUpdatePayload update = parse(rawBody);
        if (update.updateId() == null) {
            throw new BusinessRuleException("TELEGRAM_UPDATE_INVALID", "Telegram update is invalid.", 400);
        }
        TelegramUpdateRecord record = reserve(update);
        if (record.isProcessed()) {
            return;
        }
        try {
            handle(update);
            record.processed(now());
        } catch (BusinessRuleException exception) {
            respond(update, exception);
            record.processed(now());
        } catch (TelegramApiException | org.springframework.dao.TransientDataAccessException exception) {
            record.failed(exception.getClass().getSimpleName(), now());
            throw exception;
        }
    }

    private void handle(TelegramUpdatePayload update) {
        TelegramUserMode mode = mode(update);
        if (mode == TelegramUserMode.TECHNICIAN) {
            technicianBotService.handle(update);
            return;
        }
        customerBotService.handle(update);
    }

    private void respond(TelegramUpdatePayload update, BusinessRuleException exception) {
        if (currentMode(update) == TelegramUserMode.TECHNICIAN || isTechnicianCommand(update)) {
            technicianBotService.respondBusinessError(update, exception);
            return;
        }
        businessErrorResponder.respond(update, exception);
    }

    private TelegramUserMode mode(TelegramUpdatePayload update) {
        TelegramUpdatePayload.TelegramChat chat = update.chat();
        TelegramUpdatePayload.TelegramUser sender = update.sender();
        if (chat == null || sender == null || chat.id() == null) {
            return TelegramUserMode.CUSTOMER;
        }
        TelegramUserMode mode = requestedMode(update);
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

    private TelegramUpdateRecord reserve(TelegramUpdatePayload update) {
        TelegramUpdateRecord existing = updateRepository.findByTelegramUpdateIdForUpdate(update.updateId()).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == TelegramUpdateStatus.FAILED) {
                existing.retry(now());
            }
            return existing;
        }
        updateRepository.insertReceivedIfAbsent(update.updateId(), update.updateType(), now());
        return updateRepository.findByTelegramUpdateIdForUpdate(update.updateId())
                .orElseThrow(() -> new BusinessRuleException("TELEGRAM_UPDATE_INVALID", "Telegram update is invalid.", 400));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
