package com.example.darks.repair_auto.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.catalog.category.api.dto.CategoryCreateRequest;
import com.example.darks.repair_auto.catalog.category.application.RepairCategoryService;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.customer.api.dto.CustomerCreateRequest;
import com.example.darks.repair_auto.customer.application.CustomerService;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.notification.application.NotificationDeliveryService;
import com.example.darks.repair_auto.notification.infrastructure.worker.NotificationWorkerTransactions;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRequest;
import com.example.darks.repair_auto.repair.assignment.application.RepairAssignmentService;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.ObjectStorageService;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageUpload;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObject;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObjectDownload;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateRequest;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import com.example.darks.repair_auto.telegram.core.domain.TelegramUpdateStatus;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramUpdateRepository;
import com.example.darks.repair_auto.telegram.technician.api.dto.TechnicianTelegramLinkResponse;
import com.example.darks.repair_auto.telegram.technician.application.TechnicianTelegramLinkService;
import com.example.darks.repair_auto.technician.api.dto.TechnicianCreateRequest;
import com.example.darks.repair_auto.technician.application.TechnicianService;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
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
        "app.telegram.bot-username=repairauto_test_bot",
        "app.telegram.technician.bot-username=@RepairAutoStaffTestBot",
        "app.telegram.webhook-secret=test-secret"
})
@AutoConfigureMockMvc
class TelegramTechnicianBotIntegrationTest extends PostgreSqlIntegrationTest {

    private static final byte[] JPEG = new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x01};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeTelegramBotClient telegramBotClient;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RepairCategoryService categoryService;

    @Autowired
    private RepairCategoryRepository categoryRepository;

    @Autowired
    private TechnicianService technicianService;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private RepairRequestService requestService;

    @Autowired
    private RepairRequestRepository requestRepository;

    @Autowired
    private RepairAssignmentService assignmentService;

    @Autowired
    private RepairAssignmentRepository assignmentRepository;

    @Autowired
    private RepairAttachmentRepository attachmentRepository;

    @Autowired
    private TelegramUpdateRepository updateRepository;

    @Autowired
    private TechnicianTelegramLinkService linkService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NotificationDeliveryService notificationDeliveryService;

    @Autowired
    private NotificationWorkerTransactions notificationWorkerTransactions;

    @Autowired
    private ObjectStorageService objectStorageService;

    private User admin;
    private Long customerId;
    private Long categoryId;
    private Long technicianId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from telegram_user_contexts");
        jdbcTemplate.update("delete from telegram_technician_sessions");
        jdbcTemplate.update("delete from telegram_technician_link_tokens");
        jdbcTemplate.update("delete from telegram_customer_sessions");
        jdbcTemplate.update("delete from repair_request_status_history");
        jdbcTemplate.update("delete from repair_executions");
        updateRepository.deleteAll();
        attachmentRepository.deleteAll();
        assignmentRepository.deleteAll();
        requestRepository.deleteAll();
        technicianRepository.deleteAll();
        customerRepository.deleteAll();
        categoryRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        telegramBotClient.clear();
        admin = userRepository.saveAndFlush(new User(
                "Admin",
                emailNormalizer.normalize("admin-phase8@example.com"),
                passwordService.hash("AdminPass123!"),
                UserRole.ADMIN,
                true,
                OffsetDateTime.now(ZoneOffset.UTC)));
        customerId = customerService.create(new CustomerCreateRequest("Ali Valiyev", "+998901112233", LanguageCode.UZ)).id();
        categoryId = categoryService.create(new CategoryCreateRequest(
                "Washer",
                "Стиральная машина",
                "Kir yuvish mashinasi",
                null,
                null,
                null,
                true)).id();
        technicianId = technicianService.create(new TechnicianCreateRequest(
                "Technician One",
                "+998902223344",
                "Washer",
                null,
                2,
                LanguageCode.EN,
                true)).id();
    }

    @Test
    void givenTechnicianBotUsernameConfiguredWithAtSignWhenLinkCreatedThenDeepLinkOmitsAtSign() throws Exception {
        TechnicianTelegramLinkResponse link = createLink();

        assertThat(link.deepLink()).startsWith("https://t.me/RepairAutoStaffTestBot?start=tech_");
        assertThat(link.deepLink()).doesNotContain("https://t.me/@");
    }

    @Test
    void givenAdminTokenWhenTechnicianLinksAndAcceptsThenAssignmentIsAcceptedOnce() throws Exception {
        TechnicianTelegramLinkResponse link = createLink();
        String rawToken = link.deepLink().substring(link.deepLink().indexOf("tech_") + "tech_".length());

        send(update(1, 9001, 19001, "/start tech_" + rawToken));
        send(callback(2, 9001, 19001, "cb-lang", "tlang:EN"));
        send(callback(2, 9001, 19001, "cb-lang", "tlang:EN"));

        assertThat(technicianRepository.findById(technicianId).orElseThrow().getTelegramUserId()).isEqualTo(9001L);
        assertThat(updateRepository.findByTelegramUpdateId(2L).orElseThrow().getStatus())
                .isEqualTo(TelegramUpdateStatus.PROCESSED);
        assertThat(telegramBotClient.messages()).anyMatch(message -> message.text().contains("Technician profile linked"));

        Long requestId = assignedRequest();
        send(callback(3, 9001, 19001, "cb-accept", "taccept:" + requestId));
        send(callback(3, 9001, 19001, "cb-accept", "taccept:" + requestId));

        var assignment = assignmentRepository.findActiveByRequestId(
                requestId,
                RepairAssignmentRepository.ACTIVE_STATUSES).orElseThrow();
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED);
        assertThat(requestRepository.findById(requestId).orElseThrow().getStatus()).isEqualTo(RepairRequestStatus.ASSIGNED);
        assertThat(assignmentRepository.findByRepairRequestIdOrderByCreatedAtDesc(requestId)).hasSize(1);
    }

    @Test
    void givenTechnicianPhotoWhenDiagnosisPhotoUploadedThenTechnicianUploaderIsPersisted() throws Exception {
        linkTechnician(9101, 19101);
        Long requestId = assignedRequest();
        send(callback(10, 9101, 19101, "cb-accept", "taccept:" + requestId));
        send(callback(11, 9101, 19101, "cb-start", "tstart:" + requestId));
        send(callback(12, 9101, 19101, "cb-photo", "tdiagphoto:" + requestId));
        send(photo(13, 9101, 19101, "diag-photo", JPEG.length));

        var attachment = attachmentRepository.findAll().getFirst();
        assertThat(attachment.getAttachmentType()).isEqualTo(AttachmentType.DIAGNOSIS_PHOTO);
        assertThat(attachment.getUploadedByTechnician().getId()).isEqualTo(technicianId);
        assertThat(attachment.getUploadedByUser()).isNull();
        assertThat(attachment.getUploadedByCustomer()).isNull();
        assertThat(telegramBotClient.messages()).anyMatch(message -> message.text().contains("Diagnosis photo saved"));
    }

    @Test
    void givenPhase8MigrationThenCoreTablesAndConstraintsExist() {
        assertThat(tableExists("telegram_user_contexts")).isTrue();
        assertThat(tableExists("telegram_technician_link_tokens")).isTrue();
        assertThat(tableExists("telegram_technician_sessions")).isTrue();
        assertThat(columnExists("repair_attachments", "uploaded_by_technician_id")).isTrue();
        assertThat(columnExists("repair_executions", "started_by_technician_id")).isTrue();
        assertThat(constraintExists("repair_attachments", "repair_attachments_uploader_check")).isTrue();
        assertThat(indexExists("idx_telegram_technician_link_tokens_active_technician")).isTrue();
    }

    @Test
    void givenAdminUnlinksTechnicianThenTelegramLinkIsRemoved() throws Exception {
        linkTechnician(9201, 19201);

        mockMvc.perform(delete("/api/v1/technicians/{id}/telegram-link", technicianId)
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk());

        assertThat(technicianRepository.findById(technicianId).orElseThrow().getTelegramUserId()).isNull();
        send(update(20, 9201, 19201, "/technician"));
        assertThat(telegramBotClient.lastText()).contains("not linked");
    }

    @Test
    void givenModeSwitchesThenLinkedProfileIsValidatedBeforeContextChanges() throws Exception {
        send(update(30, 9301, 19301, "/technician"));

        assertThat(activeMode(9301)).isNull();
        assertThat(telegramBotClient.lastText()).contains("bog'lanmagan");

        registerCustomer(9301, 19301, "+998901112233", "Dual Profile", LanguageCode.EN);
        assertThat(activeMode(9301)).isEqualTo("CUSTOMER");

        send(update(34, 9301, 19301, "/technician"));
        assertThat(activeMode(9301)).isEqualTo("CUSTOMER");

        TechnicianTelegramLinkResponse link = createLink();
        String rawToken = rawToken(link);
        send(update(35, 9301, 19301, "/start tech_" + rawToken));
        send(callback(36, 9301, 19301, "cb-tech-lang", "tlang:EN"));
        assertThat(activeMode(9301)).isEqualTo("TECHNICIAN");

        send(update(37, 9301, 19301, "/customer"));
        assertThat(activeMode(9301)).isEqualTo("CUSTOMER");

        technicianService.changeActivation(technicianId, false, "inactive mode switch");
        send(update(38, 9301, 19301, "/technician"));

        assertThat(activeMode(9301)).isEqualTo("CUSTOMER");
        assertThat(telegramBotClient.lastText()).contains("inactive");
    }

    @Test
    void givenAlreadyLinkedTechnicianThenAnotherTelegramUserCannotTakeOverLink() throws Exception {
        linkTechnician(9401, 19401);
        TechnicianTelegramLinkResponse takeoverLink = createLink();
        String rawToken = rawToken(takeoverLink);

        send(update(40, 9402, 19402, "/start tech_" + rawToken));
        send(callback(41, 9402, 19402, "cb-takeover", "tlang:EN"));

        assertThat(technicianRepository.findById(technicianId).orElseThrow().getTelegramUserId()).isEqualTo(9401L);
        assertThat(countUsedTechnicianTokens()).isEqualTo(1);
        assertThat(telegramBotClient.lastText()).contains("bog'lab");
    }

    @Test
    void givenSameTelegramUserThenAnotherTechnicianCannotBeLinked() throws Exception {
        linkTechnician(9501, 19501);
        Long secondTechnicianId = technicianService.create(new TechnicianCreateRequest(
                "Technician Two",
                "+998902229999",
                "Washer",
                null,
                2,
                LanguageCode.EN,
                true)).id();
        TechnicianTelegramLinkResponse secondLink = createLink(secondTechnicianId);

        send(update(50, 9501, 19501, "/start tech_" + rawToken(secondLink)));
        send(callback(51, 9501, 19501, "cb-second-tech", "tlang:EN"));

        assertThat(technicianRepository.findById(secondTechnicianId).orElseThrow().getTelegramUserId()).isNull();
        assertThat(telegramBotClient.lastText()).contains("cannot be linked");
    }

    @Test
    void givenInactiveLinkedTechnicianThenTelegramActionIsRejectedWithoutBusinessEffect() throws Exception {
        linkTechnician(9601, 19601);
        Long requestId = assignedRequest();
        technicianService.changeActivation(technicianId, false, "inactive action");

        send(callback(60, 9601, 19601, "cb-inactive-accept", "taccept:" + requestId));

        var assignment = assignmentRepository.findActiveByRequestId(
                requestId,
                RepairAssignmentRepository.ACTIVE_STATUSES).orElseThrow();
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.PENDING);
        assertThat(telegramBotClient.lastText()).contains("inactive");
    }

    @Test
    void givenSameTokenConsumedConcurrentlyThenExactlyOneTechnicianLinkIsCreated() throws Exception {
        TechnicianTelegramLinkResponse link = createLink();
        String tokenHash = linkService.hash(rawToken(link));

        List<Object> results = runConcurrently(
                () -> linkService.consume(tokenHash, 9701L, 19701L, LanguageCode.EN),
                () -> linkService.consume(tokenHash, 9702L, 19702L, LanguageCode.EN));

        assertThat(results).filteredOn(result -> !(result instanceof Exception)).hasSize(1);
        assertThat(results).filteredOn(result -> result instanceof Exception).hasSize(1);
        assertThat(countLinkedTechnicians()).isEqualTo(1);
        assertThat(countUsedTechnicianTokens()).isEqualTo(1);
    }

    private TechnicianTelegramLinkResponse createLink() throws Exception {
        return createLink(technicianId);
    }

    private TechnicianTelegramLinkResponse createLink(Long targetTechnicianId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/technicians/{id}/telegram-link", targetTechnicianId)
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String deepLink = body.replaceAll(".*\"deepLink\":\"([^\"]+)\".*", "$1");
        String expiresAt = body.replaceAll(".*\"expiresAt\":\"([^\"]+)\".*", "$1");
        return new TechnicianTelegramLinkResponse(deepLink, OffsetDateTime.parse(expiresAt));
    }

    private String rawToken(TechnicianTelegramLinkResponse link) {
        return link.deepLink().substring(link.deepLink().indexOf("tech_") + "tech_".length());
    }

    private void linkTechnician(long telegramUserId, long chatId) throws Exception {
        TechnicianTelegramLinkResponse link = createLink();
        String rawToken = rawToken(link);
        send(update(100 + telegramUserId, telegramUserId, chatId, "/start tech_" + rawToken));
        send(callback(200 + telegramUserId, telegramUserId, chatId, "cb-lang-" + telegramUserId, "tlang:EN"));
    }

    private void registerCustomer(
            long userId,
            long chatId,
            String phone,
            String name,
            LanguageCode language) throws Exception {
        send(update(300 + userId, userId, chatId, "/start"));
        send(callback(301 + userId, userId, chatId, "cb-customer-lang-" + userId, "lang:" + language));
        send(update(302 + userId, userId, chatId, name));
        send(contact(303 + userId, userId, chatId, phone, userId));
    }

    private Long assignedRequest() {
        Long requestId = requestService.create(new RepairRequestCreateRequest(
                        customerId,
                        categoryId,
                        "Washer leaks water.",
                        "Tashkent",
                        null,
                        null,
                        RepairRequestPriority.NORMAL,
                        OffsetDateTime.now(ZoneOffset.UTC).plusDays(2),
                        null),
                new AuthenticatedUser(admin)).id();
        assignmentService.assign(requestId, new AssignmentRequest(technicianId, null), new AuthenticatedUser(admin));
        return requestId;
    }

    private void send(String json) throws Exception {
        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    private String update(long updateId, long userId, long chatId, String text) {
        return """
                {"update_id":%d,"message":{"message_id":%d,"from":{"id":%d,"first_name":"Tech"},"chat":{"id":%d,"type":"private"},"text":"%s"}}
                """.formatted(updateId, updateId, userId, chatId, text);
    }

    private String callback(long updateId, long userId, long chatId, String callbackId, String data) {
        return """
                {"update_id":%d,"callback_query":{"id":"%s","from":{"id":%d,"first_name":"Tech"},"message":{"message_id":%d,"chat":{"id":%d,"type":"private"}},"data":"%s"}}
                """.formatted(updateId, callbackId, userId, updateId, chatId, data);
    }

    private String photo(long updateId, long userId, long chatId, String fileId, long size) {
        return """
                {"update_id":%d,"message":{"message_id":%d,"from":{"id":%d,"first_name":"Tech"},"chat":{"id":%d,"type":"private"},"photo":[{"file_id":"%s","width":80,"height":80,"file_size":%d}]}}
                """.formatted(updateId, updateId, userId, chatId, fileId, size);
    }

    private String contact(long updateId, long userId, long chatId, String phone, long contactUserId) {
        return """
                {"update_id":%d,"message":{"message_id":%d,"from":{"id":%d},"chat":{"id":%d,"type":"private"},"contact":{"phone_number":"%s","first_name":"Contact","user_id":%d}}}
                """.formatted(updateId, updateId, userId, chatId, phone, contactUserId);
    }

    private String activeMode(long telegramUserId) {
        List<String> modes = jdbcTemplate.queryForList("""
                select active_mode from telegram_user_contexts
                where telegram_user_id = ?
                """, String.class, telegramUserId);
        return modes.isEmpty() ? null : modes.getFirst();
    }

    private int countLinkedTechnicians() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from technicians
                where telegram_user_id is not null
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private int countUsedTechnicianTokens() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from telegram_technician_link_tokens
                where used_at is not null
                """, Integer.class);
        return count == null ? 0 : count;
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

    @Test
    void givenRequestWithPhotosAndLocation_whenTechnicianAssignedAndNotificationDelivered_thenSummaryPhotosLocationAndDecisionSent() throws Exception {
        linkTechnician(112233L, 112233L);
        telegramBotClient.clear();

        var requestDetail = requestService.create(new RepairRequestCreateRequest(
                customerId,
                categoryId,
                "AC is completely broken and leaking water",
                new com.example.darks.repair_auto.repair.request.api.dto.RequestLocationRequest(
                        new java.math.BigDecimal("41.2850000"),
                        new java.math.BigDecimal("69.2050000"),
                        "Chilanzar 9, Tashkent",
                        com.example.darks.repair_auto.repair.request.domain.RequestLocationSource.DEVICE_GPS),
                null,
                null,
                null,
                RepairRequestPriority.HIGH,
                null,
                null), new AuthenticatedUser(admin));

        Long requestId = requestDetail.id();

        var customer = customerRepository.findById(customerId).orElseThrow();
        var repairRequest = requestRepository.findById(requestId).orElseThrow();
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        String validChecksum = "a".repeat(64);
        var att1 = com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment.customerUpload(
                repairRequest,
                AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                "storage-key-1",
                "problem1.jpg",
                customer,
                now);
        att1.markAvailable("image/jpeg", 1024L, validChecksum, now);
        attachmentRepository.saveAndFlush(att1);

        var att2 = com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment.customerUpload(
                repairRequest,
                AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                "storage-key-2",
                "problem2.jpg",
                customer,
                now.plusSeconds(1));
        att2.markAvailable("image/jpeg", 1024L, validChecksum, now.plusSeconds(1));
        attachmentRepository.saveAndFlush(att2);

        objectStorageService.upload(new StorageUpload("storage-key-1", "image/jpeg", JPEG.length, new ByteArrayInputStream(JPEG)));
        objectStorageService.upload(new StorageUpload("storage-key-2", "image/jpeg", JPEG.length, new ByteArrayInputStream(JPEG)));

        assignmentService.assign(requestId, new AssignmentRequest(technicianId, null), new AuthenticatedUser(admin));

        var claimedList = notificationWorkerTransactions.claim("worker-test");
        for (var claimed : claimedList) {
            notificationDeliveryService.deliver(claimed);
        }

        assertThat(telegramBotClient.messages()).isNotEmpty();
        var summaryMsg = telegramBotClient.messages().stream()
                .filter(m -> m.text().contains("🔧 New repair request") || m.text().contains("🔧 Yangi ta'mirlash") || m.text().contains("🔧 Новая заявка"))
                .findFirst()
                .orElseThrow();
        assertThat(summaryMsg.text()).contains("Request: " + requestDetail.requestNumber());
        assertThat(summaryMsg.text()).contains("Address:\nChilanzar 9, Tashkent");
        assertThat(summaryMsg.replyMarkupJson()).isNull();

        assertThat(telegramBotClient.mediaGroups()).hasSize(1);
        assertThat(telegramBotClient.mediaGroups().get(0).photos()).hasSize(2);

        assertThat(telegramBotClient.locations()).hasSize(1);
        assertThat(telegramBotClient.locations().get(0).latitude()).isEqualTo(41.285);
        assertThat(telegramBotClient.locations().get(0).longitude()).isEqualTo(69.205);

        var decisionMsg = telegramBotClient.messages().stream()
                .filter(m -> m.text().contains("accept this repair") || m.text().contains("qabul qilasizmi") || m.text().contains("принять этот ремонт"))
                .findFirst()
                .orElseThrow();
        assertThat(decisionMsg.replyMarkupJson()).contains("taccept:" + requestId);
        assertThat(decisionMsg.replyMarkupJson()).contains("treject:" + requestId);

        send(callback(50, 112233L, 112233L, "cb-accept", "taccept:" + requestId));

        var assignment = assignmentRepository.findActiveByRequestId(requestId, RepairAssignmentRepository.ACTIVE_STATUSES).orElseThrow();
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = 'public' and table_name = ? and column_name = ?
                """, Integer.class, tableName, columnName);
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
    static class TelegramTechnicianTestConfiguration {

        @Bean
        @Primary
        FakeTelegramBotClient fakeTelegramBotClient() {
            return new FakeTelegramBotClient();
        }

        @Bean("customerTelegramBotClient")
        TelegramBotClient customerTelegramBotClient(FakeTelegramBotClient fake) {
            return fake;
        }

        @Bean("technicianTelegramBotClient")
        TelegramBotClient technicianTelegramBotClient(FakeTelegramBotClient fake) {
            return fake;
        }

        @Bean("telegramBotClient")
        TelegramBotClient telegramBotClient(FakeTelegramBotClient fake) {
            return fake;
        }

        @Bean
        @Primary
        ObjectStorageService objectStorageService() {
            return new FakeObjectStorageService();
        }
    }

    static final class FakeTelegramBotClient implements TelegramBotClient {

        private final List<SentMessage> messages = new CopyOnWriteArrayList<>();
        private final List<SentPhoto> photos = new CopyOnWriteArrayList<>();
        private final List<SentMediaGroup> mediaGroups = new CopyOnWriteArrayList<>();
        private final List<SentLocation> locations = new CopyOnWriteArrayList<>();
        private final List<DeletedMessage> deletedMessages = new CopyOnWriteArrayList<>();
        private final List<EditedReplyMarkup> editedReplyMarkups = new CopyOnWriteArrayList<>();
        private final List<String> answeredCallbacks = new CopyOnWriteArrayList<>();
        private long nextMessageId = 2000L;

        void clear() {
            messages.clear();
            photos.clear();
            mediaGroups.clear();
            locations.clear();
            deletedMessages.clear();
            editedReplyMarkups.clear();
            answeredCallbacks.clear();
        }

        List<SentMessage> messages() {
            return messages;
        }

        List<SentPhoto> photos() {
            return photos;
        }

        List<SentMediaGroup> mediaGroups() {
            return mediaGroups;
        }

        List<SentLocation> locations() {
            return locations;
        }

        String lastText() {
            return messages.getLast().text();
        }

        List<DeletedMessage> deletedMessages() {
            return deletedMessages;
        }

        List<EditedReplyMarkup> editedReplyMarkups() {
            return editedReplyMarkups;
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
            deletedMessages.add(new DeletedMessage(chatId, messageId));
        }

        @Override
        public void editMessageText(Long chatId, Long messageId, String text, String replyMarkupJson) {
            messages.add(new SentMessage(chatId, text, replyMarkupJson));
        }

        @Override
        public void editMessageReplyMarkup(Long chatId, Long messageId, String replyMarkupJson) {
            editedReplyMarkups.add(new EditedReplyMarkup(chatId, messageId, replyMarkupJson));
        }

        @Override
        public TelegramFileMetadata getFile(String fileId) {
            return new TelegramFileMetadata(fileId, "photos/" + fileId + ".jpg", JPEG.length);
        }

        @Override
        public InputStream downloadFile(String filePath, long maxSizeBytes) {
            return new ByteArrayInputStream(JPEG);
        }

        @Override
        public void sendPhoto(Long chatId, String filename, byte[] photoBytes, String caption) {
            photos.add(new SentPhoto(chatId, filename, photoBytes, caption));
        }

        @Override
        public void sendMediaGroup(Long chatId, List<com.example.darks.repair_auto.telegram.core.application.TelegramMediaPhoto> photos) {
            mediaGroups.add(new SentMediaGroup(chatId, photos));
        }

        @Override
        public void sendLocation(Long chatId, double latitude, double longitude) {
            locations.add(new SentLocation(chatId, latitude, longitude));
        }
    }

    record SentMessage(Long chatId, String text, String replyMarkupJson) {
    }

    record DeletedMessage(Long chatId, Long messageId) {
    }

    record EditedReplyMarkup(Long chatId, Long messageId, String replyMarkupJson) {
    }

    record SentPhoto(Long chatId, String filename, byte[] photoBytes, String caption) {
    }

    record SentMediaGroup(Long chatId, List<com.example.darks.repair_auto.telegram.core.application.TelegramMediaPhoto> photos) {
    }

    record SentLocation(Long chatId, double latitude, double longitude) {
    }

    static final class FakeObjectStorageService implements ObjectStorageService {

        private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

        @Override
        public StoredObject upload(StorageUpload command) {
            try {
                objects.put(command.storageKey(), command.inputStream().readAllBytes());
                return new StoredObject(command.storageKey(), command.contentType(), command.sizeBytes());
            } catch (IOException exception) {
                throw new TelegramApiException("Storage failed.");
            }
        }

        @Override
        public StoredObjectDownload download(String storageKey) {
            byte[] bytes = objects.get(storageKey);
            if (bytes == null) {
                throw new TelegramApiException("Storage object not found.");
            }
            return new StoredObjectDownload("image/jpeg", bytes.length, new ByteArrayInputStream(bytes));
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
