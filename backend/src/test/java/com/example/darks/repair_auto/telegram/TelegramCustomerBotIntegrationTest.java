package com.example.darks.repair_auto.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.catalog.category.api.dto.CategoryCreateRequest;
import com.example.darks.repair_auto.catalog.category.application.RepairCategoryService;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.customer.application.CustomerService;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.ObjectStorageService;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageUpload;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObject;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestSource;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.review.infrastructure.RepairReviewRepository;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import com.example.darks.repair_auto.telegram.core.domain.TelegramUpdateStatus;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramUpdateRepository;
import com.example.darks.repair_auto.telegram.customer.infrastructure.TelegramCustomerSessionRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.telegram.enabled=true",
        "app.telegram.bot-token=test-token",
        "app.telegram.webhook-secret=test-secret",
        "app.telegram.max-pending-photos=3"
})
@AutoConfigureMockMvc
class TelegramCustomerBotIntegrationTest extends PostgreSqlIntegrationTest {

    private static final byte[] JPEG = new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x01};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeTelegramBotClient telegramBotClient;

    @Autowired
    private RepairCategoryService categoryService;

    @Autowired
    private RepairCategoryRepository categoryRepository;

    @Autowired
    private RepairRequestRepository requestRepository;

    @Autowired
    private RepairAttachmentRepository attachmentRepository;

    @Autowired
    private RepairReviewRepository reviewRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TelegramUpdateRepository updateRepository;

    @Autowired
    private TelegramCustomerSessionRepository sessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long categoryId;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        attachmentRepository.deleteAll();
        sessionRepository.deleteAll();
        updateRepository.deleteAll();
        requestRepository.deleteAll();
        customerRepository.deleteAll();
        categoryRepository.deleteAll();
        telegramBotClient.clear();
        categoryId = categoryService.create(new CategoryCreateRequest(
                "Washer",
                "Стиральная машина",
                "Kir yuvish mashinasi",
                null,
                null,
                null,
                10,
                true)).id();
    }

    @Test
    void givenMissingOrInvalidSecretWhenWebhookCalledThenUnauthorizedAndNoCookie() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update(1, 1001, 5001, "/start")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.code").value("TELEGRAM_WEBHOOK_UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update(2, 1001, 5001, "/start")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TELEGRAM_WEBHOOK_UNAUTHORIZED"));
    }

    @Test
    void givenStartAndVerifiedContactThenUzCustomerIsCreatedAndDuplicateUpdateDoesNotRepeat() throws Exception {
        send(update(10, 1001, 5001, "/start"));
        send(callback(11, 1001, 5001, "cb-lang", "lang:UZ"));
        send(update(12, 1001, 5001, "Ali Valiyev"));
        assertThat(telegramBotClient.messages().getLast().replyMarkupJson())
                .contains("\"request_contact\":true")
                .contains("Telefon raqamni ulashish");
        send(contact(13, 1001, 5001, "+998901112233", 1001));
        send(contact(13, 1001, 5001, "+998901112233", 1001));

        List<Customer> customers = customerRepository.findAll();
        assertThat(customers).hasSize(1);
        Customer customer = customers.getFirst();
        assertThat(customer.getTelegramUserId()).isEqualTo(1001);
        assertThat(customer.getTelegramChatId()).isEqualTo(5001);
        assertThat(customer.getPreferredLanguage()).isEqualTo(LanguageCode.UZ);
        assertThat(customer.getRegistrationSource().name()).isEqualTo("TELEGRAM");
        assertThat(updateRepository.findAll()).hasSize(4);
        assertThat(telegramBotClient.messages())
                .anyMatch(message -> message.replyMarkupJson() != null
                        && message.replyMarkupJson().contains("\"remove_keyboard\":true"));
        assertThat(telegramBotClient.messages()).anyMatch(message -> message.text().contains("Asosiy menyu"));
    }

    @Test
    void givenRegisteredCustomerThenMainMenuUsesReplyKeyboard() throws Exception {
        register(18018, 22018, "+998901919191", "Reply Menu", LanguageCode.EN);

        assertThat(telegramBotClient.messages().getLast().replyMarkupJson())
                .contains("\"keyboard\"")
                .contains("\"resize_keyboard\":true")
                .contains("\"is_persistent\":true")
                .doesNotContain("\"inline_keyboard\"")
                .contains("Create request")
                .contains("My requests")
                .contains("Leave a review")
                .contains("Profile")
                .contains("Change language")
                .contains("Send /cancel to reset the current draft or /menu to return to the menu.");
    }

    @Test
    void givenRegisteredCustomerWhenReplyKeyboardCreateRequestSentThenWorkflowStarts() throws Exception {
        register(19019, 23019, "+998902020202", "Reply Action", LanguageCode.EN);

        send(update(211, 19019, 23019, "Create request"));

        assertThat(telegramBotClient.lastText()).contains("Choose a repair category");
        assertThat(telegramBotClient.messages().getLast().replyMarkupJson())
                .contains("\"inline_keyboard\"")
                .contains("cat:" + categoryId);
    }

    @Test
    void givenForeignContactThenRegistrationIsRejectedWithoutCustomerCreation() throws Exception {
        send(update(20, 2002, 6002, "/start"));
        send(callback(21, 2002, 6002, "cb-lang", "lang:EN"));
        send(update(22, 2002, 6002, "Foreign Contact"));
        send(contact(23, 2002, 6002, "+998902223344", 9999));

        assertThat(customerRepository.findAll()).isEmpty();
        assertThat(telegramBotClient.lastText()).contains("belongs to this Telegram account");
    }

    @Test
    void givenTelegramWizardWithPhotoAndLocationThenRequestAndCustomerAttachmentAreCreated() throws Exception {
        register(3003, 7003, "+998903334455", "Dilshod Bot");
        send(callback(35, 3003, 7003, "cb-create", "menu:create"));
        send(callback(36, 3003, 7003, "cb-cat", "cat:" + categoryId));
        send(update(37, 3003, 7003, "Kir yuvish mashinasi suv oqizmoqda"));
        send(photo(38, 3003, 7003, "photo-file-id", JPEG.length));
        send(callback(39, 3003, 7003, "cb-skip", "photo:skip"));
        send(location(40, 3003, 7003, "41.311081", "69.240562"));
        send(callback(41, 3003, 7003, "cb-confirm", "confirm:create"));
        send(callback(41, 3003, 7003, "cb-confirm", "confirm:create"));

        var requests = requestRepository.findAll();
        assertThat(requests).hasSize(1);
        var request = requests.getFirst();
        assertThat(request.getStatus()).isEqualTo(RepairRequestStatus.NEW);
        assertThat(request.getSource()).isEqualTo(RepairRequestSource.TELEGRAM);
        assertThat(request.getCreatedByUser()).isNull();
        assertThat(request.getSourceReference()).isEqualTo("telegram-confirm-cb-confirm");
        assertThat(customerTelegramUserId(request.getId())).isEqualTo(3003);
        assertThat(attachmentRepository.findAll()).hasSize(1);
        var attachment = attachmentRepository.findAll().getFirst();
        assertThat(attachmentStatus(attachment.getId())).isEqualTo(AttachmentStatus.AVAILABLE.name());
        assertThat(attachmentUploadedByUserId(attachment.getId())).isNull();
        assertThat(attachmentUploadedByCustomerId(attachment.getId())).isEqualTo(requestCustomerId(request.getId()));
        assertThat(telegramBotClient.messages()).anyMatch(message -> message.text().contains(request.getRequestNumber()));
    }

    @Test
    void givenHistoryAndCrossCustomerCallbackThenOnlyOwnRequestIsShown() throws Exception {
        register(4004, 8004, "+998904445566", "Owner One");
        Long ownRequestId = createTelegramRequest(4004, 8004, 51, "Own request details");
        register(5005, 9005, "+998905556677", "Owner Two");
        Long otherRequestId = createTelegramRequest(5005, 9005, 61, "Other internal text");

        send(callback(70, 4004, 8004, "cb-history", "menu:history"));
        send(callback(71, 4004, 8004, "cb-open", "req:" + ownRequestId));
        send(callback(72, 4004, 8004, "cb-cross", "req:" + otherRequestId));

        assertThat(telegramBotClient.messages())
                .filteredOn(message -> message.chatId().equals(8004L))
                .anyMatch(message -> message.text().contains("Own request details"));
        assertThat(telegramBotClient.messages())
                .filteredOn(message -> message.chatId().equals(8004L))
                .noneMatch(message -> message.text().contains("Other internal text"));
        assertThat(telegramBotClient.lastText()).contains("available");
    }

    @Test
    void givenPhase7SchemaThenTelegramTablesAndUploaderConstraintsExist() {
        assertThat(tableExists("telegram_updates")).isTrue();
        assertThat(tableExists("telegram_customer_sessions")).isTrue();
        assertThat(constraintExists("telegram_updates", "telegram_updates_update_id_unique")).isTrue();
        assertThat(constraintExists("telegram_customer_sessions", "telegram_customer_sessions_user_unique")).isTrue();
        assertThat(constraintExists("repair_requests", "repair_requests_source_attribution_check")).isTrue();
        assertThat(constraintExists("repair_attachments", "repair_attachments_uploader_check")).isTrue();
        assertThat(indexExists("idx_repair_attachments_uploaded_by_customer")).isTrue();
    }

    @Test
    void givenConcurrentDuplicateConfirmationThenExactlyOneTelegramRequestIsCreated() throws Exception {
        register(6006, 10006, "+998906667788", "Concurrent User");
        send(callback(81, 6006, 10006, "cb-create-concurrent", "menu:create"));
        send(callback(82, 6006, 10006, "cb-cat-concurrent", "cat:" + categoryId));
        send(update(83, 6006, 10006, "Concurrent duplicate confirmation text"));
        send(callback(84, 6006, 10006, "cb-skip-concurrent", "photo:skip"));
        send(update(85, 6006, 10006, "Tashkent concurrent address"));

        List<Object> results = runConcurrently(
                () -> {
                    send(callback(86, 6006, 10006, "cb-confirm-concurrent", "confirm:create"));
                    return null;
                },
                () -> {
                    send(callback(86, 6006, 10006, "cb-confirm-concurrent", "confirm:create"));
                    return null;
                });

        assertThat(results).filteredOn(result -> result instanceof Exception).isEmpty();
        assertThat(requestRepository.findAll()).hasSize(1);
        assertThat(updateRepository.findAll())
                .filteredOn(record -> record.getTelegramUpdateId().equals(86L))
                .singleElement()
                .extracting(record -> record.getStatus())
                .isEqualTo(TelegramUpdateStatus.PROCESSED);
    }

    @Test
    void givenInactiveCategoryAtConfirmationThenLocalizedBusinessErrorIsSentAndProcessed() throws Exception {
        register(7007, 11007, "+998907778899", "Inactive Category");
        send(callback(91, 7007, 11007, "cb-create-inactive", "menu:create"));
        send(callback(92, 7007, 11007, "cb-cat-inactive", "cat:" + categoryId));
        send(update(93, 7007, 11007, "Inactive category confirmation text"));
        send(callback(94, 7007, 11007, "cb-skip-inactive", "photo:skip"));
        send(update(95, 7007, 11007, "Tashkent inactive category address"));
        categoryService.changeActivation(categoryId, false, "Archived between selection and confirmation.");

        send(callback(96, 7007, 11007, "cb-confirm-inactive", "confirm:create"));

        assertThat(requestRepository.findAll()).isEmpty();
        assertThat(telegramBotClient.lastText()).contains("no longer available");
        assertThat(updateRepository.findAll())
                .filteredOn(record -> record.getTelegramUpdateId().equals(96L))
                .singleElement()
                .extracting(record -> record.getStatus())
                .isEqualTo(TelegramUpdateStatus.PROCESSED);
    }

    @Test
    void givenBusinessErrorResponseDeliveryFailsThenUpdateCanRetry() throws Exception {
        send(update(101, 8008, 12008, "/start"));
        send(callback(102, 8008, 12008, "cb-lang-retry", "lang:EN"));
        telegramBotClient.failNextSend();

        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callback(103, 8008, 12008, "cb-bad-retry", "req:not-a-number")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("TELEGRAM_API_UNAVAILABLE"));

        assertThat(updateRepository.findAll())
                .filteredOn(record -> record.getTelegramUpdateId().equals(103L))
                .singleElement()
                .extracting(record -> record.getStatus())
                .isEqualTo(TelegramUpdateStatus.RECEIVED);

        send(callback(103, 8008, 12008, "cb-bad-retry", "req:not-a-number"));

        assertThat(telegramBotClient.lastText()).contains("no longer available");
        assertThat(updateRepository.findAll())
                .filteredOn(record -> record.getTelegramUpdateId().equals(103L))
                .singleElement()
                .extracting(record -> record.getStatus())
                .isEqualTo(TelegramUpdateStatus.PROCESSED);
    }

    @Test
    void givenExistingCustomerAndConflictsThenLinkingRulesAreSafe() throws Exception {
        Long existingCustomerId = customerService.create(new com.example.darks.repair_auto.customer.api.dto.CustomerCreateRequest(
                "Existing Customer",
                "+998 90 888 99 00",
                LanguageCode.RU)).id();

        register(9009, 13009, "+998908889900", "Existing Link", LanguageCode.RU);

        assertThat(customerRepository.findAll()).hasSize(1);
        Customer linked = customerRepository.findAll().getFirst();
        assertThat(linked.getId()).isEqualTo(existingCustomerId);
        assertThat(linked.getTelegramUserId()).isEqualTo(9009);
        assertThat(linked.getPreferredLanguage()).isEqualTo(LanguageCode.RU);

        send(update(111, 9010, 13010, "/start"));
        send(callback(112, 9010, 13010, "cb-lang-conflict", "lang:EN"));
        send(update(113, 9010, 13010, "Conflict User"));
        send(contact(114, 9010, 13010, "+998908889900", 9010));

        assertThat(customerRepository.findAll()).hasSize(1);
        assertThat(telegramBotClient.lastText()).contains("could not be linked");
    }

    @Test
    void givenArchivedCustomerThenTelegramRegistrationIsRejectedSafely() throws Exception {
        Long archivedCustomerId = customerService.create(new com.example.darks.repair_auto.customer.api.dto.CustomerCreateRequest(
                "Archived Customer",
                "+998 90 999 00 11",
                LanguageCode.EN)).id();
        customerService.changeActivation(archivedCustomerId, false, "Archived test customer.");

        send(update(121, 10010, 14010, "/start"));
        send(callback(122, 10010, 14010, "cb-lang-archived", "lang:EN"));
        send(update(123, 10010, 14010, "Archived Link"));
        send(contact(124, 10010, 14010, "+998909990011", 10010));

        assertThat(customerRepository.findAll()).hasSize(1);
        assertThat(customerRepository.findById(archivedCustomerId).orElseThrow().getTelegramUserId()).isNull();
        assertThat(telegramBotClient.lastText()).contains("archived");
    }

    @Test
    void givenLocalizedStatusesThenHistoryAndDetailsDoNotExposeRawEnums() throws Exception {
        register(11011, 15011, "+998901010111", "Status Localized", LanguageCode.EN);
        Long requestId = createTelegramRequest(11011, 15011, 131, "Localized status details");

        send(callback(140, 11011, 15011, "cb-history-status", "menu:history"));
        send(callback(141, 11011, 15011, "cb-details-status", "req:" + requestId));

        assertThat(telegramBotClient.messages())
                .filteredOn(message -> message.chatId().equals(15011L))
                .anyMatch(message -> message.text().contains("New"))
                .noneMatch(message -> message.text().contains("NEW"));
    }

    @Test
    void givenDuplicatePhotoUpdateThenOneAttachmentIsCreated() throws Exception {
        register(12012, 16012, "+998901212121", "Duplicate Photo", LanguageCode.EN);
        send(callback(151, 12012, 16012, "cb-create-photo-dup", "menu:create"));
        send(callback(152, 12012, 16012, "cb-cat-photo-dup", "cat:" + categoryId));
        send(update(153, 12012, 16012, "Duplicate photo request description"));
        send(photo(154, 12012, 16012, "duplicate-photo-id", JPEG.length));
        send(photo(154, 12012, 16012, "duplicate-photo-id", JPEG.length));
        send(callback(155, 12012, 16012, "cb-skip-photo-dup", "photo:skip"));
        send(update(156, 12012, 16012, "Tashkent duplicate photo address"));
        send(callback(157, 12012, 16012, "cb-confirm-photo-dup", "confirm:create"));

        assertThat(requestRepository.findAll()).hasSize(1);
        assertThat(attachmentRepository.findAll()).hasSize(1);
        assertThat(attachmentRepository.findAll().getFirst().getUploadedByCustomer().getId())
                .isEqualTo(requestCustomerId(requestRepository.findAll().getFirst().getId()));
    }

    @Test
    void givenPhotoDownloadFailureAfterRequestCreationThenPartialSuccessIsLocalized() throws Exception {
        register(13013, 17013, "+998901313131", "Photo Failure", LanguageCode.EN);
        send(callback(161, 13013, 17013, "cb-create-photo-fail", "menu:create"));
        send(callback(162, 13013, 17013, "cb-cat-photo-fail", "cat:" + categoryId));
        send(update(163, 13013, 17013, "Photo failure request description"));
        telegramBotClient.failFile("failing-photo-id");
        send(photo(164, 13013, 17013, "failing-photo-id", JPEG.length));
        send(callback(165, 13013, 17013, "cb-skip-photo-fail", "photo:skip"));
        send(update(166, 13013, 17013, "Tashkent photo failure address"));
        send(callback(167, 13013, 17013, "cb-confirm-photo-fail", "confirm:create"));

        assertThat(requestRepository.findAll()).hasSize(1);
        assertThat(attachmentRepository.findAll()).isEmpty();
        assertThat(telegramBotClient.lastText()).contains("could not be attached");
    }

    @Test
    void givenProfilePhoneConflictThenSafeLocalizedResponseIsReturned() throws Exception {
        register(14014, 18014, "+998901414141", "Profile Owner", LanguageCode.EN);
        register(14015, 18015, "+998901515151", "Phone Owner", LanguageCode.EN);

        send(callback(171, 14014, 18014, "cb-profile", "menu:profile"));
        send(callback(172, 14014, 18014, "cb-profile-phone", "profile:phone"));
        assertThat(telegramBotClient.messages().getLast().replyMarkupJson())
                .contains("\"request_contact\":true")
                .contains("Share phone number");
        send(contact(173, 14014, 18014, "+998901515151", 14014));

        assertThat(telegramBotClient.lastText()).contains("could not be linked");
        assertThat(customerRepository.findByTelegramUserId(14014L).orElseThrow().getPhone()).isEqualTo("+998901414141");
    }

    @Test
    void givenUnexpectedCallbackStateThenLocalizedInvalidActionIsReturned() throws Exception {
        register(15015, 19015, "+998901616161", "Invalid State", LanguageCode.EN);

        send(callback(181, 15015, 19015, "cb-invalid-state", "cat:" + categoryId));

        assertThat(telegramBotClient.lastText()).contains("no longer available");
        assertThat(updateRepository.findAll())
                .filteredOn(record -> record.getTelegramUpdateId().equals(181L))
                .singleElement()
                .extracting(record -> record.getStatus())
                .isEqualTo(TelegramUpdateStatus.PROCESSED);
    }

    @Test
    void givenAllRequestStatusesThenLabelsExistInEnRuAndUz() {
        var messages = new com.example.darks.repair_auto.telegram.customer.application.TelegramMessages();

        for (RepairRequestStatus status : RepairRequestStatus.values()) {
            assertThat(messages.requestStatus(status, LanguageCode.EN)).isNotBlank().isNotEqualTo(status.name());
            assertThat(messages.requestStatus(status, LanguageCode.RU)).isNotBlank().isNotEqualTo(status.name());
            assertThat(messages.requestStatus(status, LanguageCode.UZ)).isNotBlank().isNotEqualTo(status.name());
        }
    }

    @Test
    void givenCompletedRequestThenCustomerCanSubmitOneTelegramReviewAndDuplicateIsIdempotent() throws Exception {
        register(16016, 20016, "+998901717171", "Review Customer", LanguageCode.EN);
        Long requestId = seedCompletedRequest(16016, 30016);

        send(callback(191, 16016, 20016, "cb-review-menu", "menu:review"));
        assertThat(telegramBotClient.lastText()).contains("Choose a completed request");
        send(callback(192, 16016, 20016, "cb-review-req", "revreq:" + requestId));
        send(callback(193, 16016, 20016, "cb-review-rating", "revrate:5"));
        send(update(194, 16016, 20016, "Excellent service. RU: отлично. UZ: zo'r."));
        send(callback(195, 16016, 20016, "cb-review-submit", "review:submit"));
        send(callback(195, 16016, 20016, "cb-review-submit", "review:submit"));

        assertThat(reviewRepository.findAll()).hasSize(1);
        assertThat(reviewRepository.findAll().getFirst().getRating()).isEqualTo(5);
        assertThat(telegramBotClient.lastText()).contains("Thank you");

        send(callback(196, 16016, 20016, "cb-review-again", "menu:review"));
        assertThat(telegramBotClient.lastText()).contains("There are no completed requests");
    }

    @Test
    void givenCompletedRequestDetailsThenReviewButtonAndSubmittedReviewAreShown() throws Exception {
        register(17017, 21017, "+998901818181", "Review Details", LanguageCode.EN);
        Long requestId = seedCompletedRequest(17017, 30017);

        send(callback(201, 17017, 21017, "cb-details-review", "req:" + requestId));
        assertThat(telegramBotClient.messages().getLast().replyMarkupJson()).contains("revreq:" + requestId);

        send(callback(202, 17017, 21017, "cb-details-revreq", "revreq:" + requestId));
        send(callback(203, 17017, 21017, "cb-details-rate", "revrate:4"));
        send(callback(204, 17017, 21017, "cb-details-skip", "revcomment:skip"));
        send(callback(205, 17017, 21017, "cb-details-submit", "review:submit"));
        send(callback(206, 17017, 21017, "cb-details-open-reviewed", "req:" + requestId));

        assertThat(telegramBotClient.lastText()).contains("Your review: 4/5").contains("No comment");
        assertThat(telegramBotClient.messages().getLast().replyMarkupJson()).doesNotContain("revreq:" + requestId);
    }

    private void register(long userId, long chatId, String phone, String name) throws Exception {
        register(userId, chatId, phone, name, LanguageCode.EN);
    }

    private void register(long userId, long chatId, String phone, String name, LanguageCode language) throws Exception {
        long base = userId;
        send(update(base, userId, chatId, "/start"));
        send(callback(base + 1, userId, chatId, "cb-lang-" + userId, "lang:" + language));
        send(update(base + 2, userId, chatId, name));
        send(contact(base + 3, userId, chatId, phone, userId));
    }

    private Long createTelegramRequest(long userId, long chatId, long base, String description) throws Exception {
        send(callback(base, userId, chatId, "cb-create-" + base, "menu:create"));
        send(callback(base + 1, userId, chatId, "cb-cat-" + base, "cat:" + categoryId));
        send(update(base + 2, userId, chatId, description + " with enough text"));
        send(callback(base + 3, userId, chatId, "cb-skip-" + base, "photo:skip"));
        send(update(base + 4, userId, chatId, "Tashkent, customer address"));
        send(callback(base + 5, userId, chatId, "cb-confirm-" + base, "confirm:create"));
        return jdbcTemplate.queryForObject("""
                select r.id from repair_requests r
                join customers c on c.id = r.customer_id
                where c.telegram_user_id = ?
                order by r.id desc
                limit 1
                """, Long.class, userId);
    }

    private Long seedCompletedRequest(long telegramUserId, long unique) {
        Long customerId = jdbcTemplate.queryForObject("""
                select id from customers
                where telegram_user_id = ?
                """, Long.class, telegramUserId);
        Long userId = jdbcTemplate.queryForObject("""
                insert into users (full_name, email, password_hash, role, active, password_changed_at)
                values (?, ?, 'hash', 'ADMIN', true, now())
                returning id
                """, Long.class, "Review Admin " + unique, "review-admin-" + unique + "@example.com");
        Long technicianId = jdbcTemplate.queryForObject("""
                insert into technicians (
                    full_name, phone, specialization, maximum_concurrent_requests, active,
                    preferred_language, created_at, updated_at
                ) values (?, ?, 'Appliances', 5, true, 'UZ', now(), now())
                returning id
                """, Long.class, "Review Technician " + unique, String.format("+99893%07d", unique));
        Long requestId = jdbcTemplate.queryForObject("""
                insert into repair_requests (
                    request_number, customer_id, category_id, description, address, priority,
                    status, source, created_by_user_id, source_reference, created_at, updated_at
                ) values (?, ?, ?, 'Completed request for review flow.', 'Tashkent',
                    'NORMAL', 'COMPLETED', 'TELEGRAM', null, ?, now(), now())
                returning id
                """, Long.class, "REP-REVIEW-" + unique, customerId, categoryId, "review-seed-" + unique);
        jdbcTemplate.update("""
                insert into repair_assignments (
                    repair_request_id, technician_id, status, assigned_by_user_id,
                    assigned_at, responded_at, closed_at, created_at, updated_at
                ) values (?, ?, 'COMPLETED', ?, now(), now(), now(), now(), now())
                """, requestId, technicianId, userId);
        jdbcTemplate.update("""
                insert into repair_executions (
                    repair_request_id, started_at, started_by_user_id, diagnosis,
                    diagnosis_updated_at, diagnosis_updated_by_user_id, work_performed,
                    completed_at, completed_by_user_id, created_at, updated_at
                ) values (?, now(), ?, 'Seeded diagnosis.', now(), ?, 'Seeded work.',
                    now(), ?, now(), now())
                """, requestId, userId, userId, userId);
        return requestId;
    }

    private Long customerTelegramUserId(Long requestId) {
        return jdbcTemplate.queryForObject("""
                select c.telegram_user_id from repair_requests r
                join customers c on c.id = r.customer_id
                where r.id = ?
                """, Long.class, requestId);
    }

    private Long requestCustomerId(Long requestId) {
        return jdbcTemplate.queryForObject("""
                select customer_id from repair_requests
                where id = ?
                """, Long.class, requestId);
    }

    private Long attachmentUploadedByCustomerId(Long attachmentId) {
        return jdbcTemplate.queryForObject("""
                select uploaded_by_customer_id from repair_attachments
                where id = ?
                """, Long.class, attachmentId);
    }

    private Long attachmentUploadedByUserId(Long attachmentId) {
        return jdbcTemplate.queryForObject("""
                select uploaded_by_user_id from repair_attachments
                where id = ?
                """, Long.class, attachmentId);
    }

    private String attachmentStatus(Long attachmentId) {
        return jdbcTemplate.queryForObject("""
                select status from repair_attachments
                where id = ?
                """, String.class, attachmentId);
    }

    private void send(String body) throws Exception {
        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    private List<Object> runConcurrently(Callable<?> firstAction, Callable<?> secondAction) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> runAfterStart(firstAction, start));
            var second = executor.submit(() -> runAfterStart(secondAction, start));
            start.countDown();
            return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }
    }

    private Object runAfterStart(Callable<?> action, CountDownLatch start) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        try {
            action.call();
            return "SUCCESS";
        } catch (Exception exception) {
            return exception;
        }
    }

    private String update(long updateId, long userId, long chatId, String text) {
        return """
                {"update_id":%d,"message":{"message_id":%d,"from":{"id":%d,"first_name":"Test"},"chat":{"id":%d,"type":"private"},"text":"%s"}}
                """.formatted(updateId, updateId, userId, chatId, text);
    }

    private String callback(long updateId, long userId, long chatId, String callbackId, String data) {
        return """
                {"update_id":%d,"callback_query":{"id":"%s","from":{"id":%d},"message":{"message_id":%d,"chat":{"id":%d,"type":"private"}},"data":"%s"}}
                """.formatted(updateId, callbackId, userId, updateId, chatId, data);
    }

    private String contact(long updateId, long userId, long chatId, String phone, long contactUserId) {
        return """
                {"update_id":%d,"message":{"message_id":%d,"from":{"id":%d},"chat":{"id":%d,"type":"private"},"contact":{"phone_number":"%s","first_name":"Contact","user_id":%d}}}
                """.formatted(updateId, updateId, userId, chatId, phone, contactUserId);
    }

    private String location(long updateId, long userId, long chatId, String latitude, String longitude) {
        return """
                {"update_id":%d,"message":{"message_id":%d,"from":{"id":%d},"chat":{"id":%d,"type":"private"},"location":{"latitude":%s,"longitude":%s}}}
                """.formatted(updateId, updateId, userId, chatId, latitude, longitude);
    }

    private String photo(long updateId, long userId, long chatId, String fileId, int size) {
        return """
                {"update_id":%d,"message":{"message_id":%d,"from":{"id":%d},"chat":{"id":%d,"type":"private"},"photo":[{"file_id":"small","width":20,"height":20,"file_size":1},{"file_id":"%s","width":800,"height":600,"file_size":%d}]}}
                """.formatted(updateId, updateId, userId, chatId, fileId, size);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean constraintExists(String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.table_constraints
                where table_schema = 'public' and table_name = ? and constraint_name = ?
                """, Integer.class, tableName, constraintName);
        return count != null && count > 0;
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from pg_indexes
                where schemaname = 'public' and indexname = ?
                """, Integer.class, indexName);
        return count != null && count > 0;
    }

    @TestConfiguration
    static class TelegramTestConfiguration {

        @Bean({
                "fakeTelegramBotClient",
                "customerTelegramBotClient",
                "technicianTelegramBotClient",
                "telegramBotClient"
        })
        @Primary
        FakeTelegramBotClient fakeTelegramBotClient() {
            return new FakeTelegramBotClient();
        }

        @Bean
        @Primary
        ObjectStorageService objectStorageService() {
            return new FakeObjectStorageService();
        }
    }

    static final class FakeTelegramBotClient implements TelegramBotClient {

        private final List<SentMessage> messages = new CopyOnWriteArrayList<>();
        private final List<String> failingFiles = new CopyOnWriteArrayList<>();
        private boolean failNextSend;

        void clear() {
            messages.clear();
            failingFiles.clear();
            failNextSend = false;
        }

        List<SentMessage> messages() {
            return messages;
        }

        String lastText() {
            return messages.getLast().text();
        }

        void failNextSend() {
            failNextSend = true;
        }

        void failFile(String fileId) {
            failingFiles.add(fileId);
        }

        @Override
        public void sendMessage(Long chatId, String text, String replyMarkupJson) {
            if (failNextSend) {
                failNextSend = false;
                throw new TelegramApiException("Temporary Telegram send failure.");
            }
            messages.add(new SentMessage(chatId, text, replyMarkupJson));
        }

        @Override
        public void answerCallback(String callbackQueryId, String text) {
        }

        @Override
        public TelegramFileMetadata getFile(String fileId) {
            if (failingFiles.contains(fileId)) {
                throw new TelegramApiException("Temporary Telegram file lookup failure.");
            }
            return new TelegramFileMetadata(fileId, "photos/" + fileId + ".jpg", JPEG.length);
        }

        @Override
        public InputStream downloadFile(String filePath, long maxSizeBytes) {
            return new ByteArrayInputStream(JPEG);
        }
    }

    record SentMessage(Long chatId, String text, String replyMarkupJson) {
    }

    static final class FakeObjectStorageService implements ObjectStorageService {

        private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

        @Override
        public StoredObject upload(StorageUpload command) {
            try {
                objects.put(command.storageKey(), command.inputStream().readAllBytes());
                return new StoredObject(command.storageKey(), command.contentType(), command.sizeBytes());
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public URI createDownloadUrl(String storageKey, String downloadFileName, Duration ttl) {
            return URI.create("https://storage.test/download");
        }

        @Override
        public void delete(String storageKey) {
            objects.remove(storageKey);
        }

        @Override
        public boolean exists(String storageKey) {
            return objects.containsKey(storageKey);
        }
    }
}
