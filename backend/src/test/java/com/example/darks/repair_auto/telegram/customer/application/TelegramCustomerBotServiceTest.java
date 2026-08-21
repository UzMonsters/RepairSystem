package com.example.darks.repair_auto.telegram.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.customer.application.CustomerService;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCategorySummary;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestSummaryResponse;
import com.example.darks.repair_auto.repair.request.application.RepairRequestQuery;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestSource;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.review.application.RepairReviewService;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import com.example.darks.repair_auto.telegram.core.api.TelegramUpdatePayload;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramProperties;
import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSession;
import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSessionState;
import com.example.darks.repair_auto.telegram.customer.infrastructure.TelegramCustomerSessionRepository;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TelegramCustomerBotServiceTest {

    @Test
    void givenRegisteredCustomerThenMainMenuUsesReplyKeyboard() {
        TelegramKeyboards keyboards = new TelegramKeyboards();
        TelegramMessages messages = new TelegramMessages();

        String replyMarkup = keyboards.main(messages, LanguageCode.EN);

        assertThat(replyMarkup)
                .contains("\"keyboard\"")
                .contains("\"resize_keyboard\":true")
                .contains("\"is_persistent\":true")
                .doesNotContain("\"inline_keyboard\"")
                .contains("Create request")
                .contains("My requests")
                .contains("Leave a review")
                .contains("Profile")
                .contains("Change language")
                .contains("Help")
                .doesNotContain("Send /cancel to reset the current draft or /menu to return to the menu.");
    }

    @Test
    void givenRegisteredCustomerWhenReplyKeyboardCreateRequestSentThenWorkflowStarts() {
        TelegramCustomerSession session = linkedSession(19019L, 23019L);
        RepairCategory category = category();
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairCategoryRepository categories = mock(RepairCategoryRepository.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(19019L)).thenReturn(Optional.of(session));
        when(categories.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(category));
        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                categories,
                mock(RepairRequestRepository.class),
                mock(CustomerService.class),
                mock(RepairRequestService.class),
                mock(RepairReviewService.class),
                mock(TelegramCustomerPhotoService.class),
                botClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(message(211L, 19019L, 23019L, "Create request"));

        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.SELECTING_CATEGORY);
        assertThat(botClient.last().text()).contains("Choose a repair category");
        assertThat(botClient.last().replyMarkupJson())
                .contains("\"inline_keyboard\"")
                .contains("cat:123");
        assertThat(session.getActivePromptMessageId()).isEqualTo(3000L);
    }

    @Test
    void givenSelectingCategoryAndPromptMessageMatchesWhenCategoryCallbackArrivesThenAdvancesToDescription() {
        TelegramCustomerSession session = linkedSession(19119L, 23119L);
        session.state(TelegramCustomerSessionState.SELECTING_CATEGORY, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.activePromptMessageId(4444L, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        RepairCategory category = category();
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairCategoryRepository categories = mock(RepairCategoryRepository.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(19119L)).thenReturn(Optional.of(session));
        when(categories.findById(123L)).thenReturn(Optional.of(category));
        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                categories,
                mock(RepairRequestRepository.class),
                mock(CustomerService.class),
                mock(RepairRequestService.class),
                mock(RepairReviewService.class),
                mock(TelegramCustomerPhotoService.class),
                botClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(callback(221L, 19119L, 23119L, 4444L, "cb-cat-fresh", "cat:123"));

        assertThat(session.getDraftCategoryId()).isEqualTo(123L);
        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.AWAITING_DESCRIPTION);
        assertThat(botClient.last().text()).contains("Describe the problem");
        assertThat(botClient.answeredCallbacks()).contains("cb-cat-fresh");
    }

    @Test
    void givenSelectingCategoryAndPromptMessageMissingWhenCategoryCallbackArrivesThenRejectsAsStale() {
        TelegramCustomerSession session = linkedSession(19219L, 23219L);
        session.state(TelegramCustomerSessionState.SELECTING_CATEGORY, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        RepairCategory category = category();
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairCategoryRepository categories = mock(RepairCategoryRepository.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(19219L)).thenReturn(Optional.of(session));
        when(categories.findById(123L)).thenReturn(Optional.of(category));
        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                categories,
                mock(RepairRequestRepository.class),
                mock(CustomerService.class),
                mock(RepairRequestService.class),
                mock(RepairReviewService.class),
                mock(TelegramCustomerPhotoService.class),
                botClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(callback(222L, 19219L, 23219L, 5555L, "cb-cat-null-prompt", "cat:123"));

        assertThat(session.getActivePromptMessageId()).isNull();
        assertThat(session.getDraftCategoryId()).isNull();
        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.SELECTING_CATEGORY);
        assertThat(botClient.last().text()).contains("This action is no longer available");
    }

    @Test
    void givenSelectingCategoryAndPromptMessageMismatchesWhenCategoryCallbackArrivesThenRejectsAsStale() {
        TelegramCustomerSession session = linkedSession(19220L, 23220L);
        session.state(TelegramCustomerSessionState.SELECTING_CATEGORY, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.activePromptMessageId(7000L, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        RepairCategory category = category();
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairCategoryRepository categories = mock(RepairCategoryRepository.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(19220L)).thenReturn(Optional.of(session));
        when(categories.findById(123L)).thenReturn(Optional.of(category));
        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                categories,
                mock(RepairRequestRepository.class),
                mock(CustomerService.class),
                mock(RepairRequestService.class),
                mock(RepairReviewService.class),
                mock(TelegramCustomerPhotoService.class),
                botClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(callback(224L, 19220L, 23220L, 6999L, "cb-cat-old-prompt", "cat:123"));

        assertThat(session.getDraftCategoryId()).isNull();
        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.SELECTING_CATEGORY);
        assertThat(botClient.last().text()).contains("This action is no longer available");
    }

    @Test
    void givenLanguageSelectionAndPromptMessageMatchesWhenLanguageCallbackArrivesThenMainMenuIsShown() {
        TelegramCustomerSession session = linkedSession(19221L, 23221L);
        session.state(TelegramCustomerSessionState.LANGUAGE_SELECTION, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.activePromptMessageId(8000L, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        CustomerService customers = mock(CustomerService.class);
        Customer updated = Customer.telegram("Reply Action", "+998902020202", 19221L, 23221L, LanguageCode.UZ,
                OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        ReflectionTestUtils.setField(updated, "id", 77L);
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(19221L)).thenReturn(Optional.of(session));
        when(customers.updateTelegramLanguage(77L, LanguageCode.UZ)).thenReturn(updated);
        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                mock(RepairCategoryRepository.class),
                mock(RepairRequestRepository.class),
                customers,
                mock(RepairRequestService.class),
                mock(RepairReviewService.class),
                mock(TelegramCustomerPhotoService.class),
                botClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(callback(225L, 19221L, 23221L, 8000L, "cb-lang-fresh", "lang:UZ"));

        assertThat(session.getLanguage()).isEqualTo(LanguageCode.UZ);
        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.MAIN_MENU);
        assertThat(botClient.last().text()).contains("Asosiy menyu");
    }

    @Test
    void givenCategoryCallbackAfterStateAdvancedThenItIsTreatedAsStale() {
        TelegramCustomerSession session = linkedSession(19319L, 23319L);
        session.state(TelegramCustomerSessionState.AWAITING_DESCRIPTION, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.draftCategory(123L, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairCategoryRepository categories = mock(RepairCategoryRepository.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(19319L)).thenReturn(Optional.of(session));
        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                categories,
                mock(RepairRequestRepository.class),
                mock(CustomerService.class),
                mock(RepairRequestService.class),
                mock(RepairReviewService.class),
                mock(TelegramCustomerPhotoService.class),
                botClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(callback(223L, 19319L, 23319L, 6666L, "cb-cat-duplicate", "cat:999"));

        assertThat(session.getDraftCategoryId()).isEqualTo(123L);
        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.AWAITING_DESCRIPTION);
        assertThat(botClient.last().text()).contains("This action is no longer available");
        assertThat(botClient.answeredCallbacks()).contains("cb-cat-duplicate");
    }

    @Test
    void givenRegisteredCustomerWhenReplyKeyboardHelpSentThenHelpTextIsShown() {
        TelegramCustomerSession session = linkedSession(20020L, 24020L);
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(20020L)).thenReturn(Optional.of(session));
        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                mock(RepairCategoryRepository.class),
                mock(RepairRequestRepository.class),
                mock(CustomerService.class),
                mock(RepairRequestService.class),
                mock(RepairReviewService.class),
                mock(TelegramCustomerPhotoService.class),
                botClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(message(212L, 20020L, 24020L, "Help"));

        assertThat(botClient.last().text()).contains("Send /cancel to reset the current draft or /menu to return to the menu.");
        assertThat(botClient.last().replyMarkupJson()).contains("Help");
    }

    @Test
    void givenRegisteredCustomerWhenLegacyHelpTextSentThenMainKeyboardIsRefreshed() {
        TelegramCustomerSession session = linkedSession(20120L, 24120L);
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(20120L)).thenReturn(Optional.of(session));
        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                mock(RepairCategoryRepository.class),
                mock(RepairRequestRepository.class),
                mock(CustomerService.class),
                mock(RepairRequestService.class),
                mock(RepairReviewService.class),
                mock(TelegramCustomerPhotoService.class),
                botClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(message(
                214L,
                20120L,
                24120L,
                "Send /cancel to reset the current draft or /menu to return to the menu."));

        assertThat(botClient.last().text()).contains("Main menu");
        assertThat(botClient.last().replyMarkupJson())
                .contains("Help")
                .doesNotContain("Send /cancel to reset the current draft or /menu to return to the menu.");
    }

    @Test
    void givenProblemPhotoWhenAwaitingPhotosThenReceivedCountIsShown() {
        TelegramCustomerSession session = linkedSession(20220L, 24220L);
        session.language(LanguageCode.UZ, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.state(TelegramCustomerSessionState.AWAITING_PHOTO_OR_SKIP, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(20220L)).thenReturn(Optional.of(session));
        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                mock(RepairCategoryRepository.class),
                mock(RepairRequestRepository.class),
                mock(CustomerService.class),
                mock(RepairRequestService.class),
                mock(RepairReviewService.class),
                mock(TelegramCustomerPhotoService.class),
                botClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(photo(215L, 20220L, 24220L, "problem-photo", 3456L));

        assertThat(botClient.last().text()).contains("1/3 foto qabul qilindi");
        assertThat(botClient.last().replyMarkupJson()).contains("photo:skip");
        assertThat(session.photoFileIds()).containsExactly("problem-photo");
    }

    @Test
    void givenThreePhotosSentThenAutoAdvancesToLocationAndFourthPhotoIsRejected() {
        TelegramCustomerSession session = linkedSession(20320L, 24320L);
        session.language(LanguageCode.UZ, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.state(TelegramCustomerSessionState.AWAITING_PHOTO_OR_SKIP, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(20320L)).thenReturn(Optional.of(session));
        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                mock(RepairCategoryRepository.class),
                mock(RepairRequestRepository.class),
                mock(CustomerService.class),
                mock(RepairRequestService.class),
                mock(RepairReviewService.class),
                mock(TelegramCustomerPhotoService.class),
                botClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        // Photo 1
        service.handle(photo(216L, 20320L, 24320L, "photo-1", 1024L));
        assertThat(botClient.last().text()).contains("1/3 foto qabul qilindi");
        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.AWAITING_PHOTO_OR_SKIP);

        // Photo 2
        service.handle(photo(217L, 20320L, 24320L, "photo-2", 1024L));
        assertThat(botClient.last().text()).contains("2/3 foto qabul qilindi");
        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.AWAITING_PHOTO_OR_SKIP);

        // Photo 3 -> auto advance
        service.handle(photo(218L, 20320L, 24320L, "photo-3", 1024L));
        assertThat(botClient.messages()).anyMatch(m -> m.text().contains("3/3 foto qabul qilindi"));
        assertThat(botClient.last().text()).contains("Geolokatsiyani yuboring");
        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.AWAITING_LOCATION);
        assertThat(session.photoFileIds()).containsExactly("photo-1", "photo-2", "photo-3");

        // Photo 4 -> rejected
        service.handle(photo(219L, 20320L, 24320L, "photo-4", 1024L));
        assertThat(botClient.last().text()).contains("Geolokatsiyani yuboring");
        assertThat(botClient.deletedMessages()).contains(219L);
        assertThat(session.photoFileIds()).containsExactly("photo-1", "photo-2", "photo-3");
    }

    @Test
    void givenDuplicatePhotoSentThenDuplicateMessageIsShownWithoutIncreasingCount() {
        TelegramCustomerSession session = linkedSession(20420L, 24420L);
        session.language(LanguageCode.UZ, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.state(TelegramCustomerSessionState.AWAITING_PHOTO_OR_SKIP, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(20420L)).thenReturn(Optional.of(session));
        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                mock(RepairCategoryRepository.class),
                mock(RepairRequestRepository.class),
                mock(CustomerService.class),
                mock(RepairRequestService.class),
                mock(RepairReviewService.class),
                mock(TelegramCustomerPhotoService.class),
                botClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(photo(220L, 20420L, 24420L, "duplicate-photo", 1024L));
        service.handle(photo(221L, 20420L, 24420L, "duplicate-photo", 1024L));

        assertThat(botClient.last().text())
                .contains("Bu foto allaqachon biriktirilgan")
                .contains("1/3")
                .doesNotContain("foto qabul qilindi");
        assertThat(botClient.last().replyMarkupJson()).contains("photo:skip");
        assertThat(session.photoFileIds()).containsExactly("duplicate-photo");
    }

    @Test
    void givenConfirmCallbackThenSourceReferenceUsesConfirmationMessageId() {
        TelegramCustomerSession session = linkedSession(20520L, 24520L);
        session.state(TelegramCustomerSessionState.CONFIRMING_REQUEST, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.draftCategory(123L, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.draftDescription("Washer leaks water badly", OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.draftAddress("Tashkent", OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairRequestService repairRequests = mock(RepairRequestService.class);
        RepairRequest request = mock(RepairRequest.class);
        when(request.getId()).thenReturn(909L);
        when(sessions.findByTelegramUserIdForUpdate(20520L)).thenReturn(Optional.of(session));
        when(repairRequests.telegramCreate(
                eq(77L),
                eq(123L),
                eq("Washer leaks water badly"),
                eq("Tashkent"),
                any(),
                any(),
                eq("telegram-confirm-24520-9001")))
                .thenReturn(request);
        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                mock(RepairCategoryRepository.class),
                mock(RepairRequestRepository.class),
                mock(CustomerService.class),
                repairRequests,
                mock(RepairReviewService.class),
                mock(TelegramCustomerPhotoService.class),
                new RecordingTelegramBotClient(),
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(callback(230L, 20520L, 24520L, 9001L, "cb-confirm-one", "confirm:create"));

        verify(repairRequests).telegramCreate(
                eq(77L),
                eq(123L),
                eq("Washer leaks water badly"),
                eq("Tashkent"),
                any(),
                any(),
                eq("telegram-confirm-24520-9001"));
    }

    @Test
    void givenRegisteredCustomerWhenHistoryShownThenCreatedDateIsCompact() {
        TelegramCustomerSession session = linkedSession(21021L, 25021L);
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairRequestService repairRequests = mock(RepairRequestService.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(21021L)).thenReturn(Optional.of(session));
        when(repairRequests.customerHistory(eq(77L), any(RepairRequestQuery.class), any())).thenReturn(
                new PageResponse<>(
                        List.of(summary("REP-2026-000002", "2026-08-06T05:45:39.756850Z")),
                        0,
                        5,
                        1,
                        1,
                        true,
                        true));
        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                mock(RepairCategoryRepository.class),
                mock(RepairRequestRepository.class),
                mock(CustomerService.class),
                repairRequests,
                mock(RepairReviewService.class),
                mock(TelegramCustomerPhotoService.class),
                botClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(message(213L, 21021L, 25021L, "My requests"));

        assertThat(botClient.last().text())
                .contains("Washer | New | 06.08.2026 10:45")
                .doesNotContain("REP-2026-000002")
                .doesNotContain("2026-08-06T05:45:39.756850Z");
    }

    private TelegramCustomerSession linkedSession(Long userId, Long chatId) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-06T10:00:00Z");
        Customer customer = Customer.telegram("Reply Action", "+998902020202", userId, chatId, LanguageCode.EN, now);
        ReflectionTestUtils.setField(customer, "id", 77L);
        TelegramCustomerSession session = new TelegramCustomerSession(userId, chatId, now);
        session.language(LanguageCode.EN, now);
        session.linkCustomer(customer, now);
        session.state(TelegramCustomerSessionState.MAIN_MENU, now);
        return session;
    }

    private RepairCategory category() {
        RepairCategory category = new RepairCategory(
                "Washer",
                "Washer RU",
                "Washer UZ",
                "washer",
                "washer-ru",
                "washer-uz",
                null,
                null,
                null,
                true,
                OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        ReflectionTestUtils.setField(category, "id", 123L);
        return category;
    }

    private RepairRequestSummaryResponse summary(String requestNumber, String createdAt) {
        return new RepairRequestSummaryResponse(
                123L,
                requestNumber,
                RepairRequestStatus.NEW,
                RepairRequestPriority.NORMAL,
                RepairRequestSource.TELEGRAM,
                "Washer leaks water.",
                "Tashkent",
                null,
                "Customer Name",
                new RepairRequestCategorySummary(123L, "Washer UZ", "Washer description", "Washer", "Washer RU", "Washer UZ", true),
                OffsetDateTime.parse(createdAt),
                OffsetDateTime.parse(createdAt));
    }

    private TelegramUpdatePayload message(Long updateId, Long userId, Long chatId, String text) {
        return new TelegramUpdatePayload(
                updateId,
                new TelegramUpdatePayload.TelegramMessage(
                        updateId,
                        new TelegramUpdatePayload.TelegramUser(userId, "Test", null),
                        new TelegramUpdatePayload.TelegramChat(chatId, "private"),
                        text,
                        null,
                        null,
                        null),
                null);
    }

    private TelegramUpdatePayload photo(Long updateId, Long userId, Long chatId, String fileId, Long size) {
        return new TelegramUpdatePayload(
                updateId,
                new TelegramUpdatePayload.TelegramMessage(
                        updateId,
                        new TelegramUpdatePayload.TelegramUser(userId, "Test", null),
                        new TelegramUpdatePayload.TelegramChat(chatId, "private"),
                        null,
                        null,
                        null,
                        List.of(
                                new TelegramUpdatePayload.TelegramPhotoSize("small", "small-unique", 20, 20, 1L),
                                new TelegramUpdatePayload.TelegramPhotoSize(fileId, fileId + "-unique", 800, 600, size))),
                null);
    }

    private TelegramUpdatePayload callback(
            Long updateId,
            Long userId,
            Long chatId,
            Long messageId,
            String callbackId,
            String data) {
        return new TelegramUpdatePayload(
                updateId,
                null,
                new TelegramUpdatePayload.TelegramCallbackQuery(
                        callbackId,
                        new TelegramUpdatePayload.TelegramUser(userId, "Test", null),
                        new TelegramUpdatePayload.TelegramMessage(
                                messageId,
                                null,
                                new TelegramUpdatePayload.TelegramChat(chatId, "private"),
                                null,
                                null,
                                null,
                                null),
                        data));
    }

    private static final class RecordingTelegramBotClient implements TelegramBotClient {

        private final List<SentMessage> messages = new ArrayList<>();
        private final List<Long> deletedMessages = new ArrayList<>();
        private final List<String> answeredCallbacks = new ArrayList<>();
        private long nextMessageId = 3000L;

        List<SentMessage> messages() {
            return messages;
        }

        SentMessage last() {
            return messages.getLast();
        }

        List<Long> deletedMessages() {
            return deletedMessages;
        }

        List<String> answeredCallbacks() {
            return answeredCallbacks;
        }

        @Override
        public Long sendMessage(Long chatId, String text, String replyMarkupJson) {
            messages.add(new SentMessage(chatId, text, replyMarkupJson));
            return nextMessageId++;
        }

        @Override
        public void answerCallback(String callbackQueryId, String text) {
            answeredCallbacks.add(callbackQueryId);
        }

        @Override
        public void deleteMessage(Long chatId, Long messageId) {
            deletedMessages.add(messageId);
        }

        @Override
        public void editMessageText(Long chatId, Long messageId, String text, String replyMarkupJson) {
            messages.add(new SentMessage(chatId, text, replyMarkupJson));
        }

        @Override
        public void editMessageReplyMarkup(Long chatId, Long messageId, String replyMarkupJson) {
        }

        @Override
        public TelegramFileMetadata getFile(String fileId) {
            return new TelegramFileMetadata(fileId, "photos/" + fileId + ".jpg", 1024L);
        }

        @Override
        public InputStream downloadFile(String filePath, long maxSizeBytes) {
            return InputStream.nullInputStream();
        }

        @Override
        public void sendPhoto(Long chatId, String filename, byte[] photoBytes, String caption) {
        }

        @Override
        public void sendMediaGroup(Long chatId, List<com.example.darks.repair_auto.telegram.core.application.TelegramMediaPhoto> photos) {
        }

        @Override
        public void sendLocation(Long chatId, double latitude, double longitude) {
        }
    }

    private record SentMessage(Long chatId, String text, String replyMarkupJson) {
    }
}
