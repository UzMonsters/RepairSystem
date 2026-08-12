package com.example.darks.repair_auto.telegram.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.customer.application.CustomerService;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCategorySummary;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCustomerSummary;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestSummaryResponse;
import com.example.darks.repair_auto.repair.request.application.RepairRequestQuery;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
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
        when(categories.findByActiveTrueOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(category));
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
                10,
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
                new RepairRequestCustomerSummary(77L, "Reply Action", "+998902020202", LanguageCode.EN, true),
                new RepairRequestCategorySummary(123L, "Washer", "Washer RU", "Washer UZ", true, 10),
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

    private static final class RecordingTelegramBotClient implements TelegramBotClient {

        private final List<SentMessage> messages = new ArrayList<>();

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
            return new TelegramFileMetadata(fileId, "photos/" + fileId + ".jpg", 0);
        }

        @Override
        public InputStream downloadFile(String filePath, long maxSizeBytes) {
            return InputStream.nullInputStream();
        }
    }

    private record SentMessage(Long chatId, String text, String replyMarkupJson) {
    }
}
