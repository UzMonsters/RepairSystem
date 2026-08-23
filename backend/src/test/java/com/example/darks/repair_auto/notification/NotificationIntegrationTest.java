package com.example.darks.repair_auto.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.example.darks.repair_auto.notification.application.NotificationEventFactory;
import com.example.darks.repair_auto.notification.application.NotificationOutboxService;
import com.example.darks.repair_auto.notification.application.NotificationTemplateService;
import com.example.darks.repair_auto.notification.domain.NotificationAttemptOutcome;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationDeliveryAttemptRepository;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationOutboxRepository;
import com.example.darks.repair_auto.notification.infrastructure.worker.NotificationWorker;
import com.example.darks.repair_auto.notification.infrastructure.worker.NotificationWorkerTransactions;
import com.example.darks.repair_auto.notification.infrastructure.worker.PushNotificationWorkerTransactions;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.ReassignmentRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.ScheduleRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.UnassignmentRequest;
import com.example.darks.repair_auto.repair.assignment.application.RepairAssignmentService;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.execution.api.dto.CancelRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.CompleteRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.DiagnosisRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.ResumeRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.WaitForPartsRequest;
import com.example.darks.repair_auto.repair.execution.application.RepairExecutionService;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateRequest;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateResponse;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.api.dto.TechnicianCreateRequest;
import com.example.darks.repair_auto.technician.application.TechnicianService;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
        "app.notification.worker-enabled=false",
        "app.notification.initial-backoff=PT1S",
        "app.notification.max-attempts=2"
})
@AutoConfigureMockMvc
class NotificationIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepairRequestService requestService;

    @Autowired
    private RepairAssignmentService assignmentService;

    @Autowired
    private RepairExecutionService executionService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TechnicianService technicianService;

    @Autowired
    private RepairCategoryService categoryService;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @Autowired
    private NotificationDeliveryAttemptRepository attemptRepository;

    @Autowired
    private NotificationOutboxService outboxService;

    @Autowired
    private NotificationEventFactory eventFactory;

    @Autowired
    private NotificationTemplateService templateService;

    @Autowired
    private NotificationWorker worker;

    @Autowired
    private NotificationWorkerTransactions workerTransactions;

    @Autowired
    private PushNotificationWorkerTransactions pushWorkerTransactions;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private RepairRequestRepository requestRepository;

    @Autowired
    private RepairAssignmentRepository assignmentRepository;

    @Autowired
    private RepairCategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private FakeTelegramBotClient fakeTelegramBotClient;

    private User admin;
    private User manager;
    private Long customerId;
    private Long technicianId;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAll();
        requestRepository.deleteAll();
        customerRepository.deleteAll();
        technicianRepository.deleteAll();
        categoryRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        fakeTelegramBotClient.clear();
        admin = createUser("Admin", "admin@example.com", UserRole.ADMIN);
        manager = createUser("Manager", "manager@example.com", UserRole.MANAGER);
        customerId = customerService.create(new CustomerCreateRequest("Ali Valiyev", "90 111 22 33", LanguageCode.RU)).id();
        technicianId = technicianService.create(new TechnicianCreateRequest(
                "Usta Karim",
                "90 222 33 44",
                "Cooling",
                null,
                5,
                LanguageCode.EN,
                true)).id();
        categoryId = categoryService.create(new CategoryCreateRequest(
                "Air Conditioner",
                "Кондиционер",
                "Konditsioner",
                null,
                null,
                null,
                true)).id();
        linkCustomer(customerId, 10001L, 20001L, LanguageCode.RU);
        linkTechnician(technicianId, 10002L, 20002L);
    }

    @Test
    void adminCreatedRequestEnqueuesCustomerNotificationAndTelegramCreateDoesNotDuplicate() {
        var created = createRequest();

        assertThat(telegramNotifications())
                .extracting(notification -> notification.getNotificationType())
                .containsExactly(NotificationType.REQUEST_CREATED);

        requestService.telegramCreate(
                customerId,
                categoryId,
                "Telegram request with a safe customer description.",
                "Tashkent",
                null,
                null,
                "telegram-update-1");

        assertThat(telegramNotifications())
                .extracting(notification -> notification.getNotificationType())
                .containsExactly(NotificationType.REQUEST_CREATED);
        assertThat(created.requestNumber()).startsWith("REP-");
    }

    @Test
    void assignmentSchedulingAndExecutionEventsCreateExpectedOutboxRows() {
        var created = createRequest();
        assignmentService.assign(created.id(), new AssignmentRequest(technicianId, null), principal(admin));
        assignmentService.schedule(
                created.id(),
                new ScheduleRequest(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1), false),
                principal(admin));
        assignmentService.accept(created.id(), principal(admin));
        executionService.start(created.id(), principal(admin));
        executionService.updateDiagnosis(
                created.id(),
                new DiagnosisRequest("Компрессор требует обслуживания."),
                principal(admin));
        executionService.waitForParts(created.id(), new WaitForPartsRequest("Need safe internal parts note."), principal(admin));
        executionService.resume(created.id(), new ResumeRepairRequest("Parts arrived."), principal(admin));
        insertCompletionPhoto(created.id());
        executionService.complete(
                created.id(),
                new CompleteRepairRequest("Replaced damaged part.", "Ready for pickup."),
                principal(admin));

        assertThat(outboxRepository.findAll())
                .extracting(notification -> notification.getNotificationType())
                .contains(
                        NotificationType.REQUEST_CREATED,
                        NotificationType.TECHNICIAN_ASSIGNED,
                        NotificationType.TECHNICIAN_ASSIGNED,
                        NotificationType.VISIT_SCHEDULED,
                        NotificationType.VISIT_SCHEDULED,
                        NotificationType.REPAIR_STARTED,
                        NotificationType.WAITING_FOR_PARTS,
                        NotificationType.REPAIR_RESUMED,
                        NotificationType.REPAIR_COMPLETED);
    }

    @Test
    void cancellationNotifiesCustomerAndActiveTechnician() {
        var created = createRequest();
        assignmentService.assign(created.id(), new AssignmentRequest(technicianId, null), principal(admin));
        executionService.cancel(created.id(), new CancelRepairRequest("Customer cancelled."), principal(admin));

        assertThat(outboxRepository.findAll())
                .extracting(notification -> notification.getNotificationType())
                .contains(
                        NotificationType.REQUEST_CANCELLED,
                        NotificationType.REQUEST_CANCELLED);
    }

    @Test
    void workerDeliversLocalizedMessagesAndSkipsUnavailableRecipients() {
        var first = createRequest();
        assertThat(worker.runOnce()).isEqualTo(1);
        assertThat(fakeTelegramBotClient.messages()).hasSize(1);
        assertThat(fakeTelegramBotClient.messages().getFirst().text()).contains("Заявка");

        var second = createRequest();
        customerService.changeActivation(customerId, false, "archive after request");
        assertThat(worker.runOnce()).isEqualTo(1);
        assertThat(outboxRepository.findAll())
                .extracting(notification -> notification.getStatus())
                .contains(NotificationStatus.DELIVERED, NotificationStatus.SKIPPED);
        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void telegramSendOccursOutsideTransactionAndClaimCommitsBeforeSend() throws Exception {
        createRequest();
        CountDownLatch enteredSend = new CountDownLatch(1);
        CountDownLatch releaseSend = new CountDownLatch(1);
        fakeTelegramBotClient.blockNextSend(enteredSend, releaseSend);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var future = executor.submit(() -> worker.runOnce());
            assertThat(enteredSend.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(fakeTelegramBotClient.transactionStates()).containsExactly(false);
            assertThat(telegramNotifications())
                    .extracting(notification -> notification.getStatus())
                    .containsExactly(NotificationStatus.PROCESSING);
            assertThat(workerTransactions.claim("worker-b")).isEmpty();
            releaseSend.countDown();
            assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(telegramNotifications())
                    .extracting(notification -> notification.getStatus())
                    .containsExactly(NotificationStatus.DELIVERED);
        } finally {
            releaseSend.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void transientFailuresReachDeadAndDeliveredRowsAreNotClaimedOrRetried() throws Exception {
        createRequest();
        fakeTelegramBotClient.failNext("Temporary timeout.");
        worker.runOnce();
        var notification = firstTelegramNotification();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.RETRY_SCHEDULED);

        makeImmediatelyRetryable(notification.getId());
        fakeTelegramBotClient.failNext("Temporary timeout again.");
        worker.runOnce();
        notification = outboxRepository.findById(notification.getId()).orElseThrow();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DEAD);
        assertThat(workerTransactions.claim("worker-after-dead")).isEmpty();
        assertThat(attemptRepository.findByNotificationIdOrderByAttemptNumberDesc(notification.getId()))
                .extracting(attempt -> attempt.getAttemptNumber())
                .containsExactly(2, 1);

        mockMvc.perform(post("/api/v1/admin/notification-deliveries/{id}/retry", notification.getId())
                        .with(user(principal(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"retry safely\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        worker.runOnce();
        notification = outboxRepository.findById(notification.getId()).orElseThrow();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(workerTransactions.claim("worker-after-delivered")).isEmpty();

        mockMvc.perform(post("/api/v1/admin/notification-deliveries/{id}/retry", notification.getId())
                        .with(user(principal(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"retry safely\"}"))
                .andExpect(status().isConflict())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void concurrentManualRetriesConvergeAndDeliveredStateRemainsAuthoritative() throws Exception {
        createRequest();
        fakeTelegramBotClient.failNext("Temporary timeout.");
        worker.runOnce();
        var notification = firstTelegramNotification();
        makeImmediatelyRetryable(notification.getId());
        fakeTelegramBotClient.failNext("Temporary timeout again.");
        worker.runOnce();
        notification = outboxRepository.findById(notification.getId()).orElseThrow();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DEAD);

        Long failedNotificationId = notification.getId();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> retryOutcome(failedNotificationId, "first retry"));
            var second = executor.submit(() -> retryOutcome(failedNotificationId, "second retry"));
            assertThat(List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("OK", "NOTIFICATION_RETRY_NOT_ALLOWED");
        } finally {
            executor.shutdownNow();
        }
        notification = outboxRepository.findById(failedNotificationId).orElseThrow();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(attemptRepository.findByNotificationIdOrderByAttemptNumberDesc(notification.getId()))
                .extracting(attempt -> attempt.getAttemptNumber())
                .containsExactly(2, 1);

        worker.runOnce();
        notification = outboxRepository.findById(notification.getId()).orElseThrow();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
        Long deliveredId = notification.getId();
        assertThatThrownBy(() -> workerTransactions.retry(deliveredId, "delivered retry"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Delivered notifications cannot be retried.");
        assertThat(outboxRepository.findById(deliveredId).orElseThrow().getStatus())
                .isEqualTo(NotificationStatus.DELIVERED);
    }

    @Test
    void adminApiRejectsInvalidInputsAndOmitsWorkerInternals() throws Exception {
        createRequest();
        worker.runOnce();
        var notification = firstTelegramNotification();

        mockMvc.perform(get("/api/v1/notifications/{id}", notification.getId()).with(user(principal(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStartedAt").doesNotExist())
                .andExpect(jsonPath("$.processingLeaseUntil").doesNotExist())
                .andExpect(jsonPath("$.workerId").doesNotExist())
                .andExpect(jsonPath("$.providerMessageId").doesNotExist())
                .andExpect(jsonPath("$.payloadJson").doesNotExist())
                .andExpect(jsonPath("$.attempts[0].workerId").doesNotExist())
                .andExpect(jsonPath("$.attempts[0].providerMessageId").doesNotExist());

        var invalidSort = mockMvc.perform(get("/api/v1/notifications")
                        .param("sort", "payloadJson,asc")
                        .with(user(principal(admin))))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn();
        assertThat(invalidSort.getResponse().getContentAsString())
                .contains("\"traceId\":\"" + invalidSort.getResponse().getHeader("X-Trace-Id") + "\"");

        mockMvc.perform(get("/api/v1/notifications").param("size", "101").with(user(principal(admin))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/notifications").param("sort", "createdAt,sideways").with(user(principal(admin))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/notifications").param("sort", "createdAt,asc").with(user(principal(manager))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/notification-deliveries/{id}/retry", 999999L)
                        .with(user(principal(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"retry safely\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/admin/notification-deliveries/{id}/retry", notification.getId())
                        .with(user(principal(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignmentReassignmentUnassignmentNotificationMatrixIsExplicit() {
        var created = createRequest();
        Long secondTechnicianId = createTechnician("Usta Nodir", "90 222 33 45");
        linkTechnician(secondTechnicianId, 10003L, 20003L);

        assignmentService.assign(created.id(), new AssignmentRequest(technicianId, null), principal(admin));
        assignmentService.reassign(
                created.id(),
                new ReassignmentRequest(secondTechnicianId, null, "Need another specialist."),
                principal(admin));
        assignmentService.unassign(
                created.id(),
                new UnassignmentRequest("Customer requested reassessment."),
                principal(admin));

        assertNotification(created.id(), NotificationType.TECHNICIAN_ASSIGNED, customerId);
        assertNotification(created.id(), NotificationType.TECHNICIAN_ASSIGNED, technicianId);
        assertNotification(created.id(), NotificationType.TECHNICIAN_ASSIGNED, customerId);
        assertNotification(created.id(), NotificationType.TECHNICIAN_UNASSIGNED, technicianId);
        assertNotification(created.id(), NotificationType.TECHNICIAN_ASSIGNED, secondTechnicianId);
        assertNotification(created.id(), NotificationType.TECHNICIAN_UNASSIGNED, customerId);
        assertNotification(created.id(), NotificationType.TECHNICIAN_UNASSIGNED, secondTechnicianId);
    }

    @Test
    void schedulingNotificationMatrixIsExplicitAndSameValueDoesNotDuplicate() {
        var created = createRequest();
        assignmentService.assign(created.id(), new AssignmentRequest(technicianId, null), principal(admin));
        OffsetDateTime firstVisit = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).withNano(0);
        OffsetDateTime secondVisit = firstVisit.plusDays(1);

        assignmentService.schedule(created.id(), new ScheduleRequest(firstVisit, false), principal(admin));
        assignmentService.schedule(created.id(), new ScheduleRequest(secondVisit, false), principal(admin));
        long beforeSameValue = outboxRepository.count();
        assignmentService.schedule(created.id(), new ScheduleRequest(secondVisit, false), principal(admin));
        assertThat(outboxRepository.count()).isEqualTo(beforeSameValue);
        assignmentService.schedule(created.id(), new ScheduleRequest(null, true), principal(admin));

        assertNotification(created.id(), NotificationType.VISIT_SCHEDULED, customerId);
        assertNotification(created.id(), NotificationType.VISIT_SCHEDULED, technicianId);
        assertNotification(created.id(), NotificationType.VISIT_RESCHEDULED, customerId);
        assertNotification(created.id(), NotificationType.VISIT_RESCHEDULED, technicianId);
        assertNotification(created.id(), NotificationType.VISIT_CANCELLED, customerId);
        assertNotification(created.id(), NotificationType.VISIT_CANCELLED, technicianId);
    }

    @Test
    void rollbackOfBusinessTransitionsRollsBackOutboxRows() {
        assertThatThrownBy(() -> rollbackAfter(() -> createRequest()))
                .isInstanceOf(RollbackProbe.class);
        assertThat(outboxRepository.count()).isZero();
        assertThat(requestRepository.count()).isZero();

        var created = createRequest();
        long baseline = outboxRepository.count();
        assertThatThrownBy(() -> rollbackAfter(() ->
                assignmentService.assign(created.id(), new AssignmentRequest(technicianId, null), principal(admin))))
                .isInstanceOf(RollbackProbe.class);
        assertThat(outboxRepository.count()).isEqualTo(baseline);

        assignmentService.assign(created.id(), new AssignmentRequest(technicianId, null), principal(admin));
        Long secondTechnicianId = createTechnician("Usta Botir", "90 222 33 46");
        baseline = outboxRepository.count();
        assertThatThrownBy(() -> rollbackAfter(() -> assignmentService.reassign(
                created.id(),
                new ReassignmentRequest(secondTechnicianId, null, "Rollback reassignment."),
                principal(admin))))
                .isInstanceOf(RollbackProbe.class);
        assertThat(outboxRepository.count()).isEqualTo(baseline);

        baseline = outboxRepository.count();
        assertThatThrownBy(() -> rollbackAfter(() -> assignmentService.schedule(
                created.id(),
                new ScheduleRequest(OffsetDateTime.now(ZoneOffset.UTC).plusDays(2), false),
                principal(admin))))
                .isInstanceOf(RollbackProbe.class);
        assertThat(outboxRepository.count()).isEqualTo(baseline);

        assignmentService.accept(created.id(), principal(admin));
        baseline = outboxRepository.count();
        assertThatThrownBy(() -> rollbackAfter(() -> executionService.start(created.id(), principal(admin))))
                .isInstanceOf(RollbackProbe.class);
        assertThat(outboxRepository.count()).isEqualTo(baseline);

        baseline = outboxRepository.count();
        assertThatThrownBy(() -> rollbackAfter(() -> executionService.cancel(
                created.id(),
                new CancelRepairRequest("Rollback cancellation."),
                principal(admin))))
                .isInstanceOf(RollbackProbe.class);
        assertThat(outboxRepository.count()).isEqualTo(baseline);
    }

    @Test
    void twoWorkersActiveLeaseAndLeaseRecoveryRemainConsistent() throws Exception {
        createRequest();
        createRequest();
        createRequest();
        createRequest();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> workerTransactions.claim("worker-a"));
            var second = executor.submit(() -> workerTransactions.claim("worker-b"));
            var firstBatch = first.get(5, TimeUnit.SECONDS);
            var secondBatch = second.get(5, TimeUnit.SECONDS);
            Set<Long> claimedIds = new HashSet<>();
            firstBatch.forEach(notification -> claimedIds.add(notification.notificationId()));
            secondBatch.forEach(notification -> claimedIds.add(notification.notificationId()));
            assertThat(claimedIds).hasSize(firstBatch.size() + secondBatch.size());
        } finally {
            executor.shutdownNow();
        }
        assertThat(attemptRepository.count()).isZero();
        assertThat(workerTransactions.claim("worker-c")).isEmpty();

        jdbcTemplate.update("""
                update notification_outbox
                set processing_lease_until = now() - interval '1 second'
                where status = 'PROCESSING'
                """);
        assertThat(workerTransactions.claim("worker-recovery")).isNotEmpty();
        assertThat(attemptRepository.findAll())
                .extracting(attempt -> attempt.getOutcome())
                .contains(NotificationAttemptOutcome.LEASE_RECOVERED);
    }

    @Test
    void transientPermanentManualRetryAndAdminSecurityRulesWork() throws Exception {
        createRequest();
        fakeTelegramBotClient.failNext("Temporary timeout.");
        worker.runOnce();
        var notification = firstTelegramNotification();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.RETRY_SCHEDULED);

        jdbcTemplate.update(
                "update notification_outbox set next_attempt_at = now() - interval '1 second' where id = ?",
                notification.getId());
        fakeTelegramBotClient.failNext("Bad request: chat not found.");
        worker.runOnce();
        notification = outboxRepository.findById(notification.getId()).orElseThrow();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DEAD);

        mockMvc.perform(post("/api/v1/admin/notification-deliveries/{id}/retry", notification.getId())
                        .with(user(principal(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"retry safely\"}"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Set-Cookie"));

        mockMvc.perform(post("/api/v1/admin/notification-deliveries/{id}/retry", notification.getId())
                        .with(user(principal(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"retry safely\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/v1/notifications").with(user(principal(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isNotEmpty());

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void duplicateEventKeyAndTwoWorkerClaimsAreSafe() {
        var created = createRequest();
        var request = requestRepository.findWithRelationsById(created.id()).orElseThrow();
        var event = eventFactory.customer(NotificationType.REQUEST_CREATED, request, "manual-duplicate");

        outboxService.enqueue(event);
        outboxService.enqueue(event);

        assertThat(outboxRepository.countByEventKey(event.eventKey() + ":telegram")).isEqualTo(1);
        assertThat(workerTransactions.claim("worker-a")).hasSize(2);
        assertThat(workerTransactions.claim("worker-b")).isEmpty();
    }

    @Test
    void pushWorkerOverlapClaimsDoNotDuplicateNotifications() throws Exception {
        var request = createRequest();
        jdbcTemplate.update("delete from notification_delivery_attempts");
        jdbcTemplate.update("delete from notification_outbox");
        Long firstPushId = pushNotification(request.id(), "push-overlap-1");
        Long secondPushId = pushNotification(request.id(), "push-overlap-2");

        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> pushWorkerTransactions.claim("push-worker-a"));
            var second = executor.submit(() -> pushWorkerTransactions.claim("push-worker-b"));
            var firstBatch = first.get(5, TimeUnit.SECONDS);
            var secondBatch = second.get(5, TimeUnit.SECONDS);
            Set<Long> claimedIds = new HashSet<>();
            firstBatch.forEach(notification -> claimedIds.add(notification.getId()));
            secondBatch.forEach(notification -> claimedIds.add(notification.getId()));

            assertThat(claimedIds).contains(firstPushId, secondPushId);
            assertThat(claimedIds).hasSize(firstBatch.size() + secondBatch.size());
        } finally {
            executor.shutdownNow();
        }
        assertThat(pushWorkerTransactions.claim("push-worker-c")).isEmpty();
    }

    @Test
    void expiredProcessingLeaseRecoversAndAttemptsRemainAppendOnly() {
        createRequest();
        assertThat(workerTransactions.claim("worker-a")).hasSize(1);
        jdbcTemplate.update("""
                update notification_outbox
                set processing_lease_until = now() - interval '1 second'
                where status = 'PROCESSING'
                """);

        worker.runOnce();

        var notification = firstTelegramNotification();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(attemptRepository.findByNotificationIdOrderByAttemptNumberDesc(notification.getId()))
                .extracting(attempt -> attempt.getOutcome())
                .containsExactly(NotificationAttemptOutcome.DELIVERED, NotificationAttemptOutcome.LEASE_RECOVERED);
    }

    @Test
    void everyNotificationTypeHasEnRuAndUzTemplate() {
        for (NotificationType type : NotificationType.values()) {
            assertThat(templateService.hasTemplate(type, LanguageCode.EN)).isTrue();
            assertThat(templateService.hasTemplate(type, LanguageCode.RU)).isTrue();
            assertThat(templateService.hasTemplate(type, LanguageCode.UZ)).isTrue();
        }
    }

    private RepairRequestCreateResponse createRequest() {
        return requestService.create(new RepairRequestCreateRequest(
                customerId,
                categoryId,
                "The appliance starts but does not cool the room.",
                "Tashkent",
                null,
                null,
                RepairRequestPriority.NORMAL,
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(1),
                "Internal note must not be notified."),
                principal(admin));
    }

    private void insertCompletionPhoto(Long requestId) {
        jdbcTemplate.update("""
                insert into repair_attachments (
                    repair_request_id,
                    attachment_type,
                    status,
                    storage_key,
                    original_file_name,
                    content_type,
                    size_bytes,
                    sha256_checksum,
                    uploaded_by_user_id,
                    uploaded_at,
                    available_at,
                    created_at,
                    updated_at
                ) values (?, 'COMPLETION_PHOTO', 'AVAILABLE', ?, 'photo.jpg', 'image/jpeg', 10, ?, ?, now(), now(), now(), now())
                """,
                requestId,
                "completion/%d.jpg".formatted(requestId),
                "a".repeat(64),
                admin.getId());
    }

    private List<com.example.darks.repair_auto.notification.domain.NotificationOutbox> telegramNotifications() {
        return outboxRepository.findAll().stream()
                .filter(notification -> notification.getChannel() == com.example.darks.repair_auto.notification.domain.NotificationChannel.TELEGRAM)
                .toList();
    }

    private com.example.darks.repair_auto.notification.domain.NotificationOutbox firstTelegramNotification() {
        return outboxRepository.findAll().stream()
                .filter(notification -> notification.getChannel() == com.example.darks.repair_auto.notification.domain.NotificationChannel.TELEGRAM)
                .findFirst()
                .orElseThrow();
    }

    private void assertNotification(Long requestId, NotificationType type, Long recipientId) {
        String recipientType = recipientId.equals(customerId) ? "CUSTOMER" : "TECHNICIAN";
        List<NotificationRow> rows = jdbcTemplate.query("""
                select event_key, status, payload_version
                from notification_outbox
                where repair_request_id = ? and notification_type = ? and recipient_id = ? and recipient_type = ? and channel = 'TELEGRAM'
                """,
                (rs, rowNum) -> new NotificationRow(
                        rs.getString("event_key"),
                        rs.getString("status"),
                        rs.getInt("payload_version")),
                requestId,
                type.name(),
                recipientId,
                recipientType);
        assertThat(rows)
                .isNotEmpty()
                .allSatisfy(row -> {
                    assertThat(row.eventKey()).contains("request:" + requestId);
                    assertThat(row.status()).isEqualTo("PENDING");
                    assertThat(row.payloadVersion()).isEqualTo(1);
                });
    }

    private Long createTechnician(String fullName, String phone) {
        return technicianService.create(new TechnicianCreateRequest(
                fullName,
                phone,
                "Cooling",
                null,
                5,
                LanguageCode.EN,
                true)).id();
    }

    private void makeImmediatelyRetryable(Long notificationId) {
        jdbcTemplate.update(
                "update notification_outbox set next_attempt_at = now() - interval '1 second' where id = ?",
                notificationId);
    }

    private Long pushNotification(Long requestId, String eventKey) {
        return jdbcTemplate.queryForObject("""
                insert into notification_outbox (
                    event_key, notification_type, channel, recipient_type, recipient_id,
                    repair_request_id, template_key, payload_json, payload_version, language,
                    rendered_title, rendered_message, attempt_count, status,
                    next_attempt_at, created_at, updated_at
                ) values (
                    ?, 'REQUEST_CREATED', 'PUSH', 'CUSTOMER', ?,
                    ?, 'notification.request.created', '{}', 1, 'UZ',
                    'Push title', 'Push body', 0, 'PENDING',
                    now(), now(), now()
                )
                returning id
                """, Long.class, eventKey, customerId, requestId);
    }

    private void rollbackAfter(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            action.run();
            throw new RollbackProbe();
        });
    }

    private String retryOutcome(Long notificationId, String reason) {
        try {
            workerTransactions.retry(notificationId, reason);
            return "OK";
        } catch (BusinessRuleException exception) {
            return exception.code();
        }
    }

    private void linkCustomer(Long id, Long userId, Long chatId, LanguageCode language) {
        var customer = customerRepository.findById(id).orElseThrow();
        customer.linkTelegram(userId, chatId, language, OffsetDateTime.now(ZoneOffset.UTC));
        customerRepository.saveAndFlush(customer);
    }

    private void linkTechnician(Long id, Long userId, Long chatId) {
        var technician = technicianRepository.findById(id).orElseThrow();
        technician.linkTelegram(userId, chatId, OffsetDateTime.now(ZoneOffset.UTC));
        technicianRepository.saveAndFlush(technician);
    }

    private AuthenticatedUser principal(User user) {
        return new AuthenticatedUser(user);
    }

    private User createUser(String fullName, String email, UserRole role) {
        return userRepository.saveAndFlush(new User(
                fullName,
                emailNormalizer.normalize(email),
                passwordService.hash("Pass1234!"),
                role,
                true,
                OffsetDateTime.now(ZoneOffset.UTC)));
    }

    private static final class RollbackProbe extends RuntimeException {
    }

    record NotificationRow(String eventKey, String status, int payloadVersion) {
    }

    @TestConfiguration
    static class NotificationTestConfiguration {

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
    }

    static final class FakeTelegramBotClient implements TelegramBotClient {

        private final List<SentMessage> messages = new CopyOnWriteArrayList<>();
        private final List<Boolean> transactionStates = new CopyOnWriteArrayList<>();
        private final Queue<String> failures = new ArrayDeque<>();
        private final List<SentPhoto> photos = new CopyOnWriteArrayList<>();
        private final List<SentMediaGroup> mediaGroups = new CopyOnWriteArrayList<>();
        private final List<SentLocation> locations = new CopyOnWriteArrayList<>();
        private volatile CountDownLatch enteredSend;
        private volatile CountDownLatch releaseSend;

        void clear() {
            messages.clear();
            transactionStates.clear();
            failures.clear();
            photos.clear();
            mediaGroups.clear();
            locations.clear();
            enteredSend = null;
            releaseSend = null;
        }

        List<SentMessage> messages() {
            return messages;
        }

        List<Boolean> transactionStates() {
            return transactionStates;
        }

        void failNext(String message) {
            failures.add(message);
        }

        void blockNextSend(CountDownLatch enteredSend, CountDownLatch releaseSend) {
            this.enteredSend = enteredSend;
            this.releaseSend = releaseSend;
        }

        @Override
        public Long sendMessage(Long chatId, String text, String replyMarkupJson) {
            transactionStates.add(TransactionSynchronizationManager.isActualTransactionActive());
            CountDownLatch entered = enteredSend;
            CountDownLatch release = releaseSend;
            if (entered != null && release != null) {
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new TelegramApiException("Timed out waiting for test release.");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new TelegramApiException("Interrupted while waiting for test release.", exception);
                } finally {
                    enteredSend = null;
                    releaseSend = null;
                }
            }
            String failure = failures.poll();
            if (failure != null) {
                throw new TelegramApiException(failure);
            }
            messages.add(new SentMessage(chatId, text));
            return (long) messages.size();
        }

        @Override
        public Long editMessage(Long chatId, Long messageId, String text, String replyMarkupJson) {
            messages.add(new SentMessage(chatId, text));
            return messageId;
        }

        @Override
        public void answerCallback(String callbackQueryId, String text) {
        }

        @Override
        public void answerCallback(String callbackQueryId, String text, boolean showAlert) {
        }

        @Override
        public TelegramFileMetadata getFile(String fileId) {
            throw new TelegramApiException("Unsupported in notification tests.");
        }

        @Override
        public InputStream downloadFile(String filePath, long maxSizeBytes) {
            throw new TelegramApiException("Unsupported in notification tests.");
        }

        @Override
        public void sendPhoto(Long chatId, String filename, byte[] photoBytes, String caption) {
            String failure = failures.poll();
            if (failure != null) {
                throw new TelegramApiException(failure);
            }
            photos.add(new SentPhoto(chatId, filename, photoBytes, caption));
        }

        @Override
        public void sendMediaGroup(Long chatId, List<com.example.darks.repair_auto.telegram.core.application.TelegramMediaPhoto> photos) {
            String failure = failures.poll();
            if (failure != null) {
                throw new TelegramApiException(failure);
            }
            mediaGroups.add(new SentMediaGroup(chatId, photos));
        }

        @Override
        public void sendLocation(Long chatId, double latitude, double longitude) {
            String failure = failures.poll();
            if (failure != null) {
                throw new TelegramApiException(failure);
            }
            locations.add(new SentLocation(chatId, latitude, longitude));
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
    }

    record SentMessage(Long chatId, String text) {
    }

    record SentPhoto(Long chatId, String filename, byte[] photoBytes, String caption) {
    }

    record SentMediaGroup(Long chatId, List<com.example.darks.repair_auto.telegram.core.application.TelegramMediaPhoto> photos) {
    }

    record SentLocation(Long chatId, double latitude, double longitude) {
    }
}
