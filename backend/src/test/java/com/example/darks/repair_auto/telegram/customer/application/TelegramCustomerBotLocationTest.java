package com.example.darks.repair_auto.telegram.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.customer.application.CustomerService;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.review.application.RepairReviewService;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.telegram.core.api.TelegramUpdatePayload;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramProperties;
import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSession;
import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSessionState;
import com.example.darks.repair_auto.telegram.customer.infrastructure.TelegramCustomerSessionRepository;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TelegramCustomerBotLocationTest {

    private TelegramCustomerSessionRepository sessionRepository;
    private RepairCategoryRepository categoryRepository;
    private RepairRequestRepository repairRequestRepository;
    private CustomerService customerService;
    private RepairRequestService repairRequestService;
    private RepairReviewService repairReviewService;
    private TelegramCustomerPhotoService photoService;
    private RecordingTelegramBotClient botClient;
    private TelegramMessages messages;
    private TelegramKeyboards keyboards;
    private TelegramCustomerBotService botService;

    private RepairCategory category;
    private Customer customer;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(TelegramCustomerSessionRepository.class);
        categoryRepository = mock(RepairCategoryRepository.class);
        repairRequestRepository = mock(RepairRequestRepository.class);
        customerService = mock(CustomerService.class);
        repairRequestService = mock(RepairRequestService.class);
        repairReviewService = mock(RepairReviewService.class);
        photoService = mock(TelegramCustomerPhotoService.class);
        botClient = new RecordingTelegramBotClient();
        messages = new TelegramMessages();
        keyboards = new TelegramKeyboards();

        botService = new TelegramCustomerBotService(
                sessionRepository,
                categoryRepository,
                repairRequestRepository,
                customerService,
                repairRequestService,
                repairReviewService,
                photoService,
                botClient,
                messages,
                keyboards,
                new TelegramProperties(),
                ZoneId.of("Asia/Tashkent"),
                Clock.fixed(Instant.parse("2026-08-19T10:00:00Z"), ZoneOffset.UTC));

        category = mock(RepairCategory.class);
        when(category.getId()).thenReturn(10L);
        when(category.getNameEn()).thenReturn("Air Conditioner");
        when(category.getNameRu()).thenReturn("Кондиционер");
        when(category.getNameUz()).thenReturn("Konditsioner");
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        customer = new Customer("John Doe", "+998901234567", LanguageCode.EN, OffsetDateTime.parse("2026-08-19T10:00:00Z"));
        ReflectionTestUtils.setField(customer, "id", 55L);
    }

    private TelegramCustomerSession createSessionAwaitingLocation() {
        TelegramCustomerSession session = new TelegramCustomerSession(1001L, 2001L, OffsetDateTime.parse("2026-08-19T10:00:00Z"));
        session.linkCustomer(customer, OffsetDateTime.parse("2026-08-19T10:00:00Z"));
        session.language(LanguageCode.EN, OffsetDateTime.parse("2026-08-19T10:00:00Z"));
        session.draftCategory(10L, OffsetDateTime.parse("2026-08-19T10:00:00Z"));
        session.draftDescription("Brakes are squeaking", OffsetDateTime.parse("2026-08-19T10:00:00Z"));
        session.state(TelegramCustomerSessionState.AWAITING_LOCATION, OffsetDateTime.parse("2026-08-19T10:00:00Z"));
        when(sessionRepository.findByTelegramUserIdForUpdate(1001L)).thenReturn(Optional.of(session));
        return session;
    }

    @Test
    void givenAwaitingLocation_whenNativeLocationReceived_thenDraftsCoordinatesAndConfirms() {
        TelegramCustomerSession session = createSessionAwaitingLocation();

        TelegramUpdatePayload payload = new TelegramUpdatePayload(
                1L,
                new TelegramUpdatePayload.TelegramMessage(
                        101L,
                        new TelegramUpdatePayload.TelegramUser(1001L, "John", "Doe"),
                        new TelegramUpdatePayload.TelegramChat(2001L, "private"),
                        null,
                        null,
                        new TelegramUpdatePayload.TelegramLocation(new BigDecimal("41.3110810"), new BigDecimal("69.2405620")),
                        null,
                        List.of()),
                null);

        botService.handle(payload);

        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.CONFIRMING_REQUEST);
        assertThat(session.getDraftLatitude()).isEqualTo(new BigDecimal("41.3110810"));
        assertThat(session.getDraftLongitude()).isEqualTo(new BigDecimal("69.2405620"));
        assertThat(session.getDraftAddress()).isNull();

        assertThat(botClient.last().text()).contains("Confirm request");
        assertThat(botClient.last().text()).contains("Location: 📍 41.3110810, 69.2405620");
    }

    @Test
    void givenAwaitingLocation_whenEnterAddressButtonClicked_thenTransitionsToAwaitingAddress() {
        TelegramCustomerSession session = createSessionAwaitingLocation();

        TelegramUpdatePayload payload = new TelegramUpdatePayload(
                1L,
                new TelegramUpdatePayload.TelegramMessage(
                        102L,
                        new TelegramUpdatePayload.TelegramUser(1001L, "John", "Doe"),
                        new TelegramUpdatePayload.TelegramChat(2001L, "private"),
                        "⌨️ Enter address",
                        null,
                        null,
                        null,
                        List.of()),
                null);

        botService.handle(payload);

        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.AWAITING_LOCATION_ADDRESS);
        assertThat(botClient.last().text()).isEqualTo("Please enter your address:");
        assertThat(botClient.last().replyMarkupJson()).contains("\"remove_keyboard\":true");
    }

    @Test
    void givenAwaitingLocationAddress_whenAddressTextEntered_thenDraftsAddressAndConfirms() {
        TelegramCustomerSession session = createSessionAwaitingLocation();
        session.state(TelegramCustomerSessionState.AWAITING_LOCATION_ADDRESS, OffsetDateTime.parse("2026-08-19T10:00:00Z"));

        TelegramUpdatePayload payload = new TelegramUpdatePayload(
                1L,
                new TelegramUpdatePayload.TelegramMessage(
                        103L,
                        new TelegramUpdatePayload.TelegramUser(1001L, "John", "Doe"),
                        new TelegramUpdatePayload.TelegramChat(2001L, "private"),
                        "Chilanzar 9, apt 12, Tashkent",
                        null,
                        null,
                        null,
                        List.of()),
                null);

        botService.handle(payload);

        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.CONFIRMING_REQUEST);
        assertThat(session.getDraftAddress()).isEqualTo("Chilanzar 9, apt 12, Tashkent");
        assertThat(session.getDraftLatitude()).isNull();
        assertThat(session.getDraftLongitude()).isNull();

        assertThat(botClient.last().text()).contains("Confirm request");
        assertThat(botClient.last().text()).contains("Location: Chilanzar 9, apt 12, Tashkent");
    }

    @Test
    void givenAwaitingLocation_whenSkipButtonClicked_thenDraftsNullLocationAndConfirms() {
        TelegramCustomerSession session = createSessionAwaitingLocation();

        TelegramUpdatePayload payload = new TelegramUpdatePayload(
                1L,
                new TelegramUpdatePayload.TelegramMessage(
                        104L,
                        new TelegramUpdatePayload.TelegramUser(1001L, "John", "Doe"),
                        new TelegramUpdatePayload.TelegramChat(2001L, "private"),
                        "⏭ Skip",
                        null,
                        null,
                        null,
                        List.of()),
                null);

        botService.handle(payload);

        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.CONFIRMING_REQUEST);
        assertThat(session.getDraftAddress()).isNull();
        assertThat(session.getDraftLatitude()).isNull();
        assertThat(session.getDraftLongitude()).isNull();

        assertThat(botClient.last().text()).contains("Confirm request");
        assertThat(botClient.last().text()).contains("Location: Not provided");
    }

    @Test
    void givenAwaitingLocation_whenRandomTextSent_thenPromptsWithLocationGuidance() {
        TelegramCustomerSession session = createSessionAwaitingLocation();

        TelegramUpdatePayload payload = new TelegramUpdatePayload(
                1L,
                new TelegramUpdatePayload.TelegramMessage(
                        105L,
                        new TelegramUpdatePayload.TelegramUser(1001L, "John", "Doe"),
                        new TelegramUpdatePayload.TelegramChat(2001L, "private"),
                        "Hello there",
                        null,
                        null,
                        null,
                        List.of()),
                null);

        botService.handle(payload);

        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.AWAITING_LOCATION);
        assertThat(botClient.last().text()).containsIgnoringCase("location");
    }

    @Test
    void givenAwaitingLocation_whenVenueSent_thenExtractsCoordinatesAndConfirms() {
        TelegramCustomerSession session = createSessionAwaitingLocation();

        TelegramUpdatePayload payload = new TelegramUpdatePayload(
                1L,
                new TelegramUpdatePayload.TelegramMessage(
                        106L,
                        new TelegramUpdatePayload.TelegramUser(1001L, "John", "Doe"),
                        new TelegramUpdatePayload.TelegramChat(2001L, "private"),
                        null,
                        null,
                        null,
                        new TelegramUpdatePayload.TelegramVenue(
                                new TelegramUpdatePayload.TelegramLocation(new BigDecimal("41.3000000"), new BigDecimal("69.2000000")),
                                "Tashkent City",
                                "Navoi Street"),
                        List.of()),
                null);

        botService.handle(payload);

        assertThat(session.getState()).isEqualTo(TelegramCustomerSessionState.CONFIRMING_REQUEST);
        assertThat(session.getDraftLatitude()).isEqualTo(new BigDecimal("41.3000000"));
        assertThat(session.getDraftLongitude()).isEqualTo(new BigDecimal("69.2000000"));
        assertThat(botClient.last().text()).contains("Location: 📍 41.3000000, 69.2000000");
    }

    private static final class RecordingTelegramBotClient implements TelegramBotClient {

        private final List<SentMessage> messages = new ArrayList<>();
        private long nextMessageId = 5000L;

        List<SentMessage> messages() {
            return messages;
        }

        SentMessage last() {
            return messages.getLast();
        }

        @Override
        public Long sendMessage(Long chatId, String text, String replyMarkupJson) {
            messages.add(new SentMessage(chatId, text, replyMarkupJson));
            return nextMessageId++;
        }

        @Override
        public void answerCallback(String callbackQueryId, String text) {
        }

        @Override
        public void deleteMessage(Long chatId, Long messageId) {
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
