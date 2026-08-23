package com.example.darks.repair_auto.telegram.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCustomerSummary;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestDetailResponse;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestSummaryResponse;
import com.example.darks.repair_auto.repair.request.application.RepairRequestQuery;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestSource;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
        assertThat(botClient.last().text()).contains("Maksimal 3 ta foto qabul qilinadi");
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
    void givenRegisteredCustomerWhenHistoryShownThenEachRequestHasOwnButtonAndNoGenericOpen() {
        TelegramCustomerSession session = linkedSession(21021L, 25021L);
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairRequestService repairRequests = mock(RepairRequestService.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(21021L)).thenReturn(Optional.of(session));
        when(repairRequests.customerHistory(eq(77L), any(RepairRequestQuery.class), any())).thenReturn(
                new PageResponse<>(
                        List.of(
                                summary(101L, "REP-2026-000001", "Washer", RepairRequestStatus.NEW, "2026-08-06T05:45:39Z"),
                                summary(102L, "REP-2026-000002", "Laptop", RepairRequestStatus.IN_PROGRESS, "2026-08-05T05:45:39Z"),
                                summary(103L, "REP-2026-000003", "Phone", RepairRequestStatus.COMPLETED, "2026-08-04T05:45:39Z")),
                        0,
                        5,
                        3,
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
                .contains("Select a request to view:")
                .doesNotContain("REP-2026-000001")
                .doesNotContain("101")
                .doesNotContain("|");
        assertThat(botClient.last().replyMarkupJson())
                .contains("req:101:0")
                .contains("🆕 Washer · 06.08.2026")
                .contains("req:102:0")
                .contains("🛠 Laptop · 05.08.2026")
                .contains("req:103:0")
                .contains("✅ Phone · 04.08.2026")
                .contains("🏠 Main menu")
                .doesNotContain("Open")
                .doesNotContain("Ochish")
                .doesNotContain("Открыть")
                .doesNotContain("REP-2026");
    }

    @Test
    void givenMultipleRequestsWhenExactRequestClickedThenExactDetailsAreRendered() {
        TelegramCustomerSession session = linkedSession(21021L, 25021L);
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairRequestRepository requestRepository = mock(RepairRequestRepository.class);
        RepairRequestService repairRequests = mock(RepairRequestService.class);
        RepairReviewService reviewService = mock(RepairReviewService.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        RepairRequest requestEntity = mock(RepairRequest.class);

        when(sessions.findByTelegramUserIdForUpdate(21021L)).thenReturn(Optional.of(session));
        when(requestRepository.findByIdAndCustomerId(102L, 77L)).thenReturn(Optional.of(requestEntity));
        when(repairRequests.get(102L)).thenReturn(detail(102L, "REP-2026-000002", "Laptop", RepairRequestStatus.IN_PROGRESS, "Broken screen", "Tashkent, Chilanzar"));

        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                mock(RepairCategoryRepository.class),
                requestRepository,
                mock(CustomerService.class),
                repairRequests,
                reviewService,
                mock(TelegramCustomerPhotoService.class),
                botClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(callback(214L, 21021L, 25021L, 9002L, "cb-req-102", "req:102:0"));

        assertThat(botClient.last().text())
                .contains("🔧 Laptop")
                .contains("🛠 In progress")
                .contains("📝 Problem")
                .contains("Broken screen")
                .contains("📍 Location")
                .contains("Tashkent, Chilanzar")
                .contains("🕒 Created")
                .contains("06.08.2026, 10:45")
                .doesNotContain("REP-2026-000002")
                .doesNotContain("102")
                .doesNotContain("Washer");
        assertThat(botClient.last().replyMarkupJson())
                .contains("hist:0")
                .contains("◀️ Back to my requests")
                .contains("🏠 Main menu");
    }

    @Test
    void givenSessionInNonHistoryStateWhenValidRequestCallbackReceivedThenDetailsRenderedStatelessly() {
        TelegramCustomerSession session = linkedSession(21021L, 25021L);
        session.state(TelegramCustomerSessionState.CONFIRMING_REQUEST, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairRequestRepository requestRepository = mock(RepairRequestRepository.class);
        RepairRequestService repairRequests = mock(RepairRequestService.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        RepairRequest requestEntity = mock(RepairRequest.class);

        when(sessions.findByTelegramUserIdForUpdate(21021L)).thenReturn(Optional.of(session));
        when(requestRepository.findByIdAndCustomerId(101L, 77L)).thenReturn(Optional.of(requestEntity));
        when(repairRequests.get(101L)).thenReturn(detail(101L, "REP-2026-000001", "Washer", RepairRequestStatus.NEW, "Leaking pump", null));

        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                mock(RepairCategoryRepository.class),
                requestRepository,
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

        service.handle(callback(215L, 21021L, 25021L, 9003L, "cb-req-101", "req:101:0"));

        assertThat(botClient.last().text())
                .contains("🔧 Washer")
                .contains("🆕 New")
                .contains("Leaking pump");
    }

    @Test
    void givenOtherCustomerRequestWhenCallbackReceivedThenDeniedGracefullyWithoutLeak() {
        TelegramCustomerSession session = linkedSession(21021L, 25021L);
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairRequestRepository requestRepository = mock(RepairRequestRepository.class);
        RepairRequestService repairRequests = mock(RepairRequestService.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();

        when(sessions.findByTelegramUserIdForUpdate(21021L)).thenReturn(Optional.of(session));
        when(requestRepository.findByIdAndCustomerId(999L, 77L)).thenReturn(Optional.empty());

        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                mock(RepairCategoryRepository.class),
                requestRepository,
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

        service.handle(callback(216L, 21021L, 25021L, 9004L, "cb-req-cross", "req:999:0"));

        assertThat(botClient.last().text()).contains("This action is no longer available.");
        assertThat(botClient.last().text()).doesNotContain("REP-");
    }

    @Test
    void givenPaginationWhenHistoryRequestedThenPreviousAndNextControlsAreAccurate() {
        TelegramCustomerSession session = linkedSession(21021L, 25021L);
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairRequestService repairRequests = mock(RepairRequestService.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(21021L)).thenReturn(Optional.of(session));

        // Page 0 with next
        when(repairRequests.customerHistory(eq(77L), any(RepairRequestQuery.class), eq(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))))
                .thenReturn(new PageResponse<>(
                        List.of(summary(101L, "REP-2026-000001", "Washer", RepairRequestStatus.NEW, "2026-08-06T05:45:39Z")),
                        0, 5, 8, 2, true, false));

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

        service.handle(message(217L, 21021L, 25021L, "My requests"));

        assertThat(botClient.last().replyMarkupJson())
                .contains("Next ▶️")
                .contains("hist:1")
                .doesNotContain("Previous")
                .doesNotContain("Oldingi");

        // Page 1 with prev, no next
        when(repairRequests.customerHistory(eq(77L), any(RepairRequestQuery.class), eq(PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "createdAt")))))
                .thenReturn(new PageResponse<>(
                        List.of(summary(106L, "REP-2026-000006", "Washer", RepairRequestStatus.NEW, "2026-08-01T05:45:39Z")),
                        1, 5, 8, 2, false, true));

        service.handle(callback(218L, 21021L, 25021L, 9005L, "cb-hist-1", "hist:1"));

        assertThat(botClient.last().replyMarkupJson())
                .contains("◀️ Previous")
                .contains("hist:0")
                .doesNotContain("Next ▶️");
    }

    @Test
    void givenBackButtonPressedFromDetailsThenOriginatingPagePreserved() {
        TelegramCustomerSession session = linkedSession(21021L, 25021L);
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairRequestRepository requestRepository = mock(RepairRequestRepository.class);
        RepairRequestService repairRequests = mock(RepairRequestService.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        RepairRequest requestEntity = mock(RepairRequest.class);

        when(sessions.findByTelegramUserIdForUpdate(21021L)).thenReturn(Optional.of(session));
        when(requestRepository.findByIdAndCustomerId(108L, 77L)).thenReturn(Optional.of(requestEntity));
        when(repairRequests.get(108L)).thenReturn(detail(108L, "REP-2026-000008", "Phone", RepairRequestStatus.NEW, "Mic broken", null));

        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                mock(RepairCategoryRepository.class),
                requestRepository,
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

        // Open request from page 2
        service.handle(callback(219L, 21021L, 25021L, 9006L, "cb-req-page-2", "req:108:2"));

        assertThat(botClient.last().replyMarkupJson())
                .contains("hist:2")
                .contains("◀️ Back to my requests");

        // Click Back
        when(repairRequests.customerHistory(eq(77L), any(RepairRequestQuery.class), eq(PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "createdAt")))))
                .thenReturn(new PageResponse<>(
                        List.of(summary(108L, "REP-2026-000008", "Phone", RepairRequestStatus.NEW, "2026-08-01T05:45:39Z")),
                        2, 5, 12, 3, false, false));

        service.handle(callback(220L, 21021L, 25021L, 9007L, "cb-back-page-2", "hist:2"));

        assertThat(botClient.last().text()).contains("Select a request to view:");
        assertThat(botClient.last().replyMarkupJson()).contains("req:108:2");
    }

    @Test
    void givenEmptyCustomerHistoryWhenMyRequestsCalledThenEmptyStateRendered() {
        TelegramCustomerSession session = linkedSession(21021L, 25021L);
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairRequestService repairRequests = mock(RepairRequestService.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(21021L)).thenReturn(Optional.of(session));
        when(repairRequests.customerHistory(eq(77L), any(RepairRequestQuery.class), any())).thenReturn(
                new PageResponse<>(List.of(), 0, 5, 0, 0, true, true));

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

        service.handle(message(221L, 21021L, 25021L, "My requests"));

        assertThat(botClient.last().text())
                .contains("📋 My requests")
                .contains("You do not have repair requests yet.");
        assertThat(botClient.last().replyMarkupJson())
                .contains("🏠 Main menu")
                .doesNotContain("req:")
                .doesNotContain("hist:");
    }

    @Test
    void givenCompletedEligibleRequestWhenDetailsShownThenLeaveReviewActionIsPresent() {
        TelegramCustomerSession session = linkedSession(21021L, 25021L);
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairRequestRepository requestRepository = mock(RepairRequestRepository.class);
        RepairRequestService repairRequests = mock(RepairRequestService.class);
        RepairReviewService reviewService = mock(RepairReviewService.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        RepairRequest requestEntity = mock(RepairRequest.class);

        when(sessions.findByTelegramUserIdForUpdate(21021L)).thenReturn(Optional.of(session));
        when(requestRepository.findByIdAndCustomerId(103L, 77L)).thenReturn(Optional.of(requestEntity));
        when(repairRequests.get(103L)).thenReturn(detail(103L, "REP-2026-000003", "Phone", RepairRequestStatus.COMPLETED, "Battery replaced", null));
        when(reviewService.customerReview(77L, 103L)).thenReturn(null);
        when(reviewService.canReview(77L, 103L)).thenReturn(true);

        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                mock(RepairCategoryRepository.class),
                requestRepository,
                mock(CustomerService.class),
                repairRequests,
                reviewService,
                mock(TelegramCustomerPhotoService.class),
                botClient,
                new TelegramMessages(),
                new TelegramKeyboards(),
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(callback(222L, 21021L, 25021L, 9008L, "cb-req-103", "req:103:0"));

        assertThat(botClient.last().replyMarkupJson())
                .contains("⭐ Leave a review")
                .contains("revreq:103");
    }

    @Test
    void givenMalformedCallbackThenThrowsInvalidCallbackException() {
        TelegramCustomerSession session = linkedSession(21021L, 25021L);
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(21021L)).thenReturn(Optional.of(session));

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

        assertThatThrownBy(() -> service.handle(callback(223L, 21021L, 25021L, 9009L, "cb-malformed", "req:notanumber")))
                .isInstanceOf(com.example.darks.repair_auto.shared.error.BusinessRuleException.class)
                .hasMessageContaining("Invalid callback");
    }

    @Test
    void givenUzbekLanguageWhenMyRequestsAndDetailsShownThenUzbekFormattingUsed() {
        TelegramCustomerSession session = linkedSession(21021L, 25021L);
        session.language(LanguageCode.UZ, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        TelegramCustomerSessionRepository sessions = mock(TelegramCustomerSessionRepository.class);
        RepairRequestRepository requestRepository = mock(RepairRequestRepository.class);
        RepairRequestService repairRequests = mock(RepairRequestService.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        RepairRequest requestEntity = mock(RepairRequest.class);

        when(sessions.findByTelegramUserIdForUpdate(21021L)).thenReturn(Optional.of(session));
        when(repairRequests.customerHistory(eq(77L), any(RepairRequestQuery.class), any())).thenReturn(
                new PageResponse<>(
                        List.of(summary(101L, "REP-2026-000001", "Washer", RepairRequestStatus.COMPLETED, "2026-08-06T05:45:39Z")),
                        0, 5, 1, 1, true, true));
        when(requestRepository.findByIdAndCustomerId(101L, 77L)).thenReturn(Optional.of(requestEntity));
        when(repairRequests.get(101L)).thenReturn(detail(101L, "REP-2026-000001", "Washer", RepairRequestStatus.COMPLETED, "Batareyka yetib keldi", "Toshkent shahri"));

        TelegramCustomerBotService service = new TelegramCustomerBotService(
                sessions,
                mock(RepairCategoryRepository.class),
                requestRepository,
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

        service.handle(message(224L, 21021L, 25021L, "Mening arizalarim"));

        assertThat(botClient.last().text())
                .contains("📋 Mening arizalarim")
                .contains("Arizani ko‘rish uchun tanlang:");
        assertThat(botClient.last().replyMarkupJson())
                .contains("✅ Washer UZ · 06.08.2026")
                .contains("🏠 Asosiy menyu");

        service.handle(callback(225L, 21021L, 25021L, 9010L, "cb-req-uz", "req:101:0"));

        assertThat(botClient.last().text())
                .contains("🔧 Washer UZ")
                .contains("✅ Yakunlangan")
                .contains("📝 Muammo")
                .contains("Batareyka yetib keldi")
                .contains("📍 Manzil")
                .contains("Toshkent shahri")
                .contains("🕒 Yaratilgan");
        assertThat(botClient.last().replyMarkupJson())
                .contains("◀️ Arizalarimga qaytish")
                .contains("🏠 Asosiy menyu");
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

    private RepairRequestSummaryResponse summary(
            Long id,
            String requestNumber,
            String categoryName,
            RepairRequestStatus status,
            String createdAt) {
        return new RepairRequestSummaryResponse(
                id,
                requestNumber,
                status,
                RepairRequestPriority.NORMAL,
                RepairRequestSource.TELEGRAM,
                categoryName + " problem description.",
                "Tashkent",
                null,
                "Customer Name",
                new RepairRequestCategorySummary(123L, categoryName + " UZ", categoryName + " description", categoryName, categoryName + " RU", categoryName + " UZ", true),
                OffsetDateTime.parse(createdAt),
                OffsetDateTime.parse(createdAt));
    }

    private RepairRequestDetailResponse detail(
            Long id,
            String requestNumber,
            String categoryName,
            RepairRequestStatus status,
            String description,
            String address) {
        return new RepairRequestDetailResponse(
                id,
                requestNumber,
                status,
                RepairRequestPriority.NORMAL,
                RepairRequestSource.TELEGRAM,
                description,
                address,
                null,
                null,
                null,
                null,
                new RepairRequestCustomerSummary(77L, "Customer Name", "+998902020202", LanguageCode.EN, true),
                new RepairRequestCategorySummary(123L, categoryName + " UZ", categoryName + " description", categoryName, categoryName + " RU", categoryName + " UZ", true),
                null,
                null,
                null,
                OffsetDateTime.parse("2026-08-06T05:45:39Z"),
                OffsetDateTime.parse("2026-08-06T05:45:39Z"));
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

        List<SentMessage> messages() {
            return messages;
        }

        SentMessage last() {
            return messages.getLast();
        }

        @Override
        public void sendMessage(Long chatId, String text, String replyMarkupJson) {
            messages.add(new SentMessage(chatId, text, replyMarkupJson));
        }

        @Override
        public void answerCallback(String callbackQueryId, String text) {
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
