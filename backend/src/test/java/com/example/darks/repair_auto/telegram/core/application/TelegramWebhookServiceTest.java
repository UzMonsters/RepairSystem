package com.example.darks.repair_auto.telegram.core.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.telegram.core.api.TelegramUpdatePayload;
import com.example.darks.repair_auto.telegram.core.domain.TelegramUserContext;
import com.example.darks.repair_auto.telegram.core.domain.TelegramUserMode;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramUpdateRepository;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramUserContextRepository;
import com.example.darks.repair_auto.telegram.customer.application.TelegramBusinessErrorResponder;
import com.example.darks.repair_auto.telegram.customer.application.TelegramCustomerBotService;
import com.example.darks.repair_auto.telegram.technician.application.TelegramTechnicianBotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class TelegramWebhookServiceTest {

    @Test
    void givenRequestedModeThenContextSwitchUsesLockedLookupInsideTransaction() {
        TelegramUserContextRepository contexts = mock(TelegramUserContextRepository.class);
        TelegramCustomerBotService customerBotService = mock(TelegramCustomerBotService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        TelegramUserContext context = new TelegramUserContext(
                101L,
                201L,
                TelegramUserMode.TECHNICIAN,
                OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        when(contexts.findByTelegramUserIdForUpdate(101L)).thenReturn(Optional.of(context));
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        TelegramWebhookService service = new TelegramWebhookService(
                mock(TelegramUpdateRepository.class),
                contexts,
                customerBotService,
                mock(TelegramTechnicianBotService.class),
                mock(TelegramBusinessErrorResponder.class),
                new ObjectMapper(),
                transactionTemplate,
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        TelegramUserMode mode = ReflectionTestUtils.invokeMethod(
                service,
                "mode",
                message("/customer"),
                null);

        verify(customerBotService).requireSwitchAllowed(101L, 201L);
        verify(transactionTemplate).executeWithoutResult(any());
        verify(contexts).findByTelegramUserIdForUpdate(101L);
        verify(contexts, never()).findByTelegramUserId(101L);
        org.assertj.core.api.Assertions.assertThat(mode).isEqualTo(TelegramUserMode.CUSTOMER);
        org.assertj.core.api.Assertions.assertThat(context.getActiveMode()).isEqualTo(TelegramUserMode.CUSTOMER);
    }

    @Test
    void givenForcedCustomerStartThenLinkedCustomerGuardIsNotRequired() {
        TelegramUserContextRepository contexts = mock(TelegramUserContextRepository.class);
        TelegramCustomerBotService customerBotService = mock(TelegramCustomerBotService.class);
        TransactionTemplate transactionTemplate = transactionTemplate();
        when(contexts.findByTelegramUserIdForUpdate(101L)).thenReturn(Optional.empty());
        doAnswer(invocation -> invocation.getArgument(0, TelegramUserContext.class))
                .when(contexts).saveAndFlush(any(TelegramUserContext.class));
        TelegramWebhookService service = service(contexts, customerBotService, transactionTemplate);

        TelegramUserMode mode = ReflectionTestUtils.invokeMethod(
                service,
                "mode",
                message("/start"),
                TelegramUserMode.CUSTOMER);

        verify(customerBotService, never()).requireSwitchAllowed(101L, 201L);
        verify(transactionTemplate).executeWithoutResult(any());
        org.assertj.core.api.Assertions.assertThat(mode).isEqualTo(TelegramUserMode.CUSTOMER);
    }

    @Test
    void givenForcedCustomerLanguageCallbackThenLinkedCustomerGuardIsNotRequired() {
        TelegramUserContextRepository contexts = mock(TelegramUserContextRepository.class);
        TelegramCustomerBotService customerBotService = mock(TelegramCustomerBotService.class);
        TransactionTemplate transactionTemplate = transactionTemplate();
        when(contexts.findByTelegramUserIdForUpdate(101L)).thenReturn(Optional.empty());
        doAnswer(invocation -> invocation.getArgument(0, TelegramUserContext.class))
                .when(contexts).saveAndFlush(any(TelegramUserContext.class));
        TelegramWebhookService service = service(contexts, customerBotService, transactionTemplate);

        TelegramUserMode mode = ReflectionTestUtils.invokeMethod(
                service,
                "mode",
                callback("lang:UZ"),
                TelegramUserMode.CUSTOMER);

        verify(customerBotService, never()).requireSwitchAllowed(101L, 201L);
        verify(transactionTemplate).executeWithoutResult(any());
        org.assertj.core.api.Assertions.assertThat(mode).isEqualTo(TelegramUserMode.CUSTOMER);
    }

    @Test
    void givenForcedCustomerRegistrationTextThenLinkedCustomerGuardIsNotRequired() {
        TelegramUserContextRepository contexts = mock(TelegramUserContextRepository.class);
        TelegramCustomerBotService customerBotService = mock(TelegramCustomerBotService.class);
        TransactionTemplate transactionTemplate = transactionTemplate();
        when(contexts.findByTelegramUserIdForUpdate(101L)).thenReturn(Optional.empty());
        doAnswer(invocation -> invocation.getArgument(0, TelegramUserContext.class))
                .when(contexts).saveAndFlush(any(TelegramUserContext.class));
        TelegramWebhookService service = service(contexts, customerBotService, transactionTemplate);

        TelegramUserMode mode = ReflectionTestUtils.invokeMethod(
                service,
                "mode",
                message("Sarvar Ro'ziboyev"),
                TelegramUserMode.CUSTOMER);

        verify(customerBotService, never()).requireSwitchAllowed(101L, 201L);
        verify(transactionTemplate).executeWithoutResult(any());
        org.assertj.core.api.Assertions.assertThat(mode).isEqualTo(TelegramUserMode.CUSTOMER);
    }

    private TelegramWebhookService service(
            TelegramUserContextRepository contexts,
            TelegramCustomerBotService customerBotService,
            TransactionTemplate transactionTemplate) {
        return new TelegramWebhookService(
                mock(TelegramUpdateRepository.class),
                contexts,
                customerBotService,
                mock(TelegramTechnicianBotService.class),
                mock(TelegramBusinessErrorResponder.class),
                new ObjectMapper(),
                transactionTemplate,
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));
    }

    private TransactionTemplate transactionTemplate() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        return transactionTemplate;
    }

    private TelegramUpdatePayload message(String text) {
        return new TelegramUpdatePayload(
                1L,
                new TelegramUpdatePayload.TelegramMessage(
                        1L,
                        new TelegramUpdatePayload.TelegramUser(101L, "User", null),
                        new TelegramUpdatePayload.TelegramChat(201L, "private"),
                        text,
                        null,
                        null,
                        null),
                null);
    }

    private TelegramUpdatePayload callback(String data) {
        TelegramUpdatePayload.TelegramMessage message = new TelegramUpdatePayload.TelegramMessage(
                1L,
                new TelegramUpdatePayload.TelegramUser(101L, "User", null),
                new TelegramUpdatePayload.TelegramChat(201L, "private"),
                null,
                null,
                null,
                null);
        return new TelegramUpdatePayload(
                1L,
                null,
                new TelegramUpdatePayload.TelegramCallbackQuery(
                        "callback-id",
                        new TelegramUpdatePayload.TelegramUser(101L, "User", null),
                        message,
                        data));
    }
}
