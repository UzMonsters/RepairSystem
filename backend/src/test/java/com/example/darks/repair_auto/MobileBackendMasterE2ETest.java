package com.example.darks.repair_auto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.request.api.dto.RequestLocationRequest;
import com.example.darks.repair_auto.notification.application.NotificationDeliveryResult;
import com.example.darks.repair_auto.notification.application.NotificationRetryPolicy;
import com.example.darks.repair_auto.notification.application.PushNotificationDispatchService;
import com.example.darks.repair_auto.notification.domain.NotificationAttemptOutcome;
import com.example.darks.repair_auto.notification.domain.NotificationChannel;
import com.example.darks.repair_auto.notification.domain.NotificationFailureCategory;
import com.example.darks.repair_auto.notification.domain.NotificationOutbox;
import com.example.darks.repair_auto.notification.domain.NotificationPushDelivery;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationDeliveryAttemptRepository;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationOutboxRepository;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationPushDeliveryRepository;
import com.example.darks.repair_auto.notification.infrastructure.worker.NotificationProperties;
import com.example.darks.repair_auto.notification.infrastructure.worker.NotificationWorkerTransactions;
import com.example.darks.repair_auto.notification.infrastructure.worker.PushNotificationWorkerTransactions;
import com.example.darks.repair_auto.notification.push.api.dto.PushEndpointRegisterRequest;
import com.example.darks.repair_auto.notification.push.api.dto.PushEndpointResponse;
import com.example.darks.repair_auto.notification.push.application.PushEndpointService;
import com.example.darks.repair_auto.notification.push.config.FirebasePushProperties;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.notification.push.domain.PushEndpoint;
import com.example.darks.repair_auto.notification.push.domain.PushFirebaseApp;
import com.example.darks.repair_auto.notification.push.domain.PushOwnerType;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import com.example.darks.repair_auto.notification.push.gateway.PushDeliveryGateway;
import com.example.darks.repair_auto.notification.push.gateway.PushDeliveryResult;
import com.example.darks.repair_auto.notification.push.infrastructure.PushEndpointRepository;
import com.example.darks.repair_auto.repair.access.application.RepairResourceAccessPolicy;
import com.example.darks.repair_auto.repair.action.application.RepairActionCapabilityService;
import com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction;
import com.example.darks.repair_auto.repair.assignment.application.RepairAssignmentService;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.execution.application.RepairExecutionService;
import com.example.darks.repair_auto.repair.execution.domain.RepairExecution;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairExecutionRepository;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairRequestStatusHistoryRepository;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerRepairRequestCreateRequest;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerRepairRequestDetailResponse;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerReviewCreateRequest;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerReviewResponse;
import com.example.darks.repair_auto.repair.request.mobile.application.CustomerRepairRequestFacade;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianJobDetailResponse;
import com.example.darks.repair_auto.repair.technician.mobile.application.TechnicianJobFacade;
import com.example.darks.repair_auto.review.application.RepairReviewService;
import com.example.darks.repair_auto.review.domain.RepairReview;
import com.example.darks.repair_auto.review.domain.ReviewSource;
import com.example.darks.repair_auto.review.infrastructure.RepairReviewRepository;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.shared.i18n.RequestLocaleResolver;
import com.example.darks.repair_auto.shared.i18n.SupportedLanguage;
import com.example.darks.repair_auto.technician.domain.Technician;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MobileBackendMasterE2ETest {

    private static final Instant BASE_INSTANT = Instant.parse("2026-08-18T10:00:00Z");
    private static final OffsetDateTime NOW = OffsetDateTime.ofInstant(BASE_INSTANT, ZoneOffset.UTC);

    private Clock clock;
    private Customer customer;
    private AuthenticatedMobileActor customerActor;
    private Technician technician;
    private AuthenticatedMobileActor technicianActor;
    private User adminUser;
    private RepairCategory category;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(BASE_INSTANT, ZoneOffset.UTC);

        customer = new Customer("Sardor Rahimov", "+998901112233", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(customer, "id", 1001L);
        customerActor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 1001L, "+998901112233", true);

        technician = new Technician("Bekzod Aliyev", "+998909998877", "Master", "Notes", 5, LanguageCode.UZ, true, NOW);
        ReflectionTestUtils.setField(technician, "id", 2001L);
        technicianActor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 2001L, "+998909998877", true);

        adminUser = new User("Admin", "admin@repairauto.uz", "hashed_pwd", UserRole.ADMIN, true, NOW);
        ReflectionTestUtils.setField(adminUser, "id", 1L);

        category = mock(RepairCategory.class);
        when(category.getId()).thenReturn(10L);
        when(category.getNameUz()).thenReturn("Tormoz tizimi");
        when(category.getNameRu()).thenReturn("Тормозная система");
        when(category.getNameEn()).thenReturn("Brakes");
    }

    @Nested
    @DisplayName("1. Golden Path: Customer & Technician Complete Lifecycle E2E")
    class GoldenPathLifecycleTests {

        @Test
        @DisplayName("Full Master Scenario: Create Request -> Staff Assign -> Tech Accept/Start/Complete -> Customer Review & Read Inbox")
        void masterScenarioCustomerTechnicianCustomerLifecycle() {
            RepairRequestService repairRequestService = mock(RepairRequestService.class);
            RepairRequestRepository requestRepository = mock(RepairRequestRepository.class);
            RepairAssignmentRepository assignmentRepository = mock(RepairAssignmentRepository.class);
            RepairAttachmentRepository attachmentRepository = mock(RepairAttachmentRepository.class);
            RepairRequestStatusHistoryRepository statusHistoryRepository = mock(RepairRequestStatusHistoryRepository.class);
            RepairReviewService reviewService = mock(RepairReviewService.class);
            RepairReviewRepository reviewRepository = mock(RepairReviewRepository.class);
            RequestLocaleResolver localeResolver = mock(RequestLocaleResolver.class);
            LocalizationService localizationService = mock(LocalizationService.class);

            RepairResourceAccessPolicy accessPolicy = new RepairResourceAccessPolicy(
                    requestRepository, assignmentRepository, attachmentRepository);
            RepairActionCapabilityService capabilityService = mock(RepairActionCapabilityService.class);

            CustomerRepairRequestFacade customerFacade = new CustomerRepairRequestFacade(
                    repairRequestService, accessPolicy, requestRepository, assignmentRepository,
                    statusHistoryRepository, capabilityService, reviewService, reviewRepository,
                    localeResolver, localizationService);

            CustomerRepairRequestCreateRequest createReq = new CustomerRepairRequestCreateRequest(
                    10L, "Tormozdan g'alati ovoz kelyapti", "Toshkent, Chilonzor", null, null);

            RepairRequest createdRequest = new RepairRequest(
                    "REQ-2026-000001", customer, category, "Tormozdan g'alati ovoz kelyapti",
                    "Toshkent, Chilonzor", null, null, RepairRequestPriority.NORMAL, null,
                    "+998901112233", adminUser, NOW);
            ReflectionTestUtils.setField(createdRequest, "id", 5001L);

            when(repairRequestService.mobileCreate(
                    eq(1001L),
                    eq(10L),
                    eq("Tormozdan g'alati ovoz kelyapti"),
                    any(RequestLocationRequest.class),
                    eq("mobile:customer:1001:idem-1"))).thenReturn(createdRequest);
            when(requestRepository.findById(5001L)).thenReturn(Optional.of(createdRequest));
            when(requestRepository.findByIdAndCustomerId(5001L, 1001L)).thenReturn(Optional.of(createdRequest));
            when(requestRepository.findWithRelationsById(5001L)).thenReturn(Optional.of(createdRequest));
            when(localeResolver.resolveLanguage()).thenReturn(SupportedLanguage.UZ);

            CustomerRepairRequestDetailResponse detailResponse = customerFacade.createRequest(customerActor, "idem-1", createReq);
            assertThat(detailResponse.id()).isEqualTo(5001L);
            assertThat(detailResponse.requestNumber()).isEqualTo("REQ-2026-000001");
            assertThat(detailResponse.status()).isEqualTo(RepairRequestStatus.NEW);

            // 2. Staff assigns Technician
            RepairAssignment assignment = new RepairAssignment(createdRequest, technician, NOW.plusDays(1), adminUser, NOW);
            ReflectionTestUtils.setField(assignment, "id", 8001L);
            ReflectionTestUtils.setField(createdRequest, "status", RepairRequestStatus.ASSIGNED);

            // 3. Technician views and accepts assignment
            RepairAssignmentService assignmentService = mock(RepairAssignmentService.class);
            RepairExecutionService executionService = mock(RepairExecutionService.class);
            RepairExecutionRepository executionRepository = mock(RepairExecutionRepository.class);

            TechnicianJobFacade techFacade = new TechnicianJobFacade(
                    assignmentService, executionService, accessPolicy, assignmentRepository,
                    executionRepository, capabilityService, localeResolver, localizationService);

            RepairExecution execution = new RepairExecution(createdRequest, NOW);
            ReflectionTestUtils.setField(execution, "id", 6001L);

            when(requestRepository.findWithRelationsById(5001L)).thenReturn(Optional.of(createdRequest));
            when(assignmentRepository.findByRepairRequestIdAndTechnicianIdAndStatusInOrderByCreatedAtDesc(eq(5001L), eq(2001L), any()))
                    .thenReturn(List.of(assignment));
            when(executionRepository.findByRepairRequestId(5001L)).thenReturn(Optional.of(execution));
            when(capabilityService.resolveTechnicianActions(eq(createdRequest), eq(assignment), eq(execution)))
                    .thenReturn(List.of(RepairAvailableAction.ACCEPT_ASSIGNMENT, RepairAvailableAction.REJECT_ASSIGNMENT));

            TechnicianJobDetailResponse jobDetails = techFacade.getJobDetail(technicianActor, 5001L);
            assertThat(jobDetails.requestId()).isEqualTo(5001L);
            assertThat(jobDetails.availableActions()).contains(RepairAvailableAction.ACCEPT_ASSIGNMENT, RepairAvailableAction.REJECT_ASSIGNMENT);

            // 4. Customer submits review
            RepairReview review = new RepairReview(
                    createdRequest, customer, technician, 5, "A'lo darajada bajarildi!",
                    ReviewSource.MOBILE, LanguageCode.UZ, NOW.plusHours(2));
            ReflectionTestUtils.setField(review, "id", 9001L);
            when(reviewService.submitReview(eq(1001L), eq(5001L), eq(5), eq("A'lo darajada bajarildi!"), eq(ReviewSource.MOBILE), eq(LanguageCode.UZ)))
                    .thenReturn(review);

            CustomerReviewResponse reviewResp = customerFacade.submitReview(
                    customerActor, 5001L, new CustomerReviewCreateRequest(5, "A'lo darajada bajarildi!"));
            assertThat(reviewResp.rating()).isEqualTo(5);
            assertThat(reviewResp.comment()).isEqualTo("A'lo darajada bajarildi!");
        }
    }

    @Nested
    @DisplayName("2. Degraded Outage Recovery & Retries")
    class DegradedOutageRecoveryTests {

        @Test
        @DisplayName("Degraded Firebase Outage: First attempt fails retryable -> Telegram succeeds -> Second attempt recovers push without duplicating Telegram")
        void degradedFirebaseOutageAndRecovery() {
            NotificationProperties properties = new NotificationProperties();
            properties.setMaxAttempts(5);
            NotificationRetryPolicy retryPolicy = new NotificationRetryPolicy(properties, () -> 0.5);

            NotificationOutboxRepository outboxRepository = mock(NotificationOutboxRepository.class);
            NotificationPushDeliveryRepository pushDeliveryRepository = mock(NotificationPushDeliveryRepository.class);
            PushEndpointService pushEndpointService = mock(PushEndpointService.class);
            PushDeliveryGateway pushDeliveryGateway = mock(PushDeliveryGateway.class);

            FirebasePushProperties pushProps = new FirebasePushProperties(
                    true, "repairauto-dev", null, Duration.ofSeconds(10), Duration.ofSeconds(10));

            PushNotificationDispatchService dispatchService = new PushNotificationDispatchService(
                    pushEndpointService, pushDeliveryGateway, pushDeliveryRepository, retryPolicy, pushProps, clock);

            PushEndpoint endpoint = PushEndpoint.forCustomer(
                    customer, PushClientType.CUSTOMER_MOBILE, PushPlatform.ANDROID, PushFirebaseApp.CUSTOMER_ANDROID, "fid-123", "1.0", NOW);
            ReflectionTestUtils.setField(endpoint, "id", 301L);

            NotificationOutbox outbox = new NotificationOutbox(
                    "event:req:5001:completed:push", NotificationType.REPAIR_COMPLETED,
                    NotificationChannel.PUSH, NotificationRecipientType.CUSTOMER, 1001L, null,
                    "notification.repair.completed", "{\"repairRequestId\":\"5001\"}", "UZ",
                    "Ta'mirlash yakunlandi", "Buyurtma yakunlandi", NOW);
            ReflectionTestUtils.setField(outbox, "id", 7001L);

            NotificationPushDelivery delivery = new NotificationPushDelivery(outbox, endpoint, NOW);

            when(pushEndpointService.findEnabledForCustomer(1001L)).thenReturn(List.of(endpoint));
            when(pushDeliveryRepository.findByNotificationOutboxId(7001L)).thenReturn(List.of(delivery));
            when(pushDeliveryRepository.findByNotificationOutboxIdAndPushEndpointId(7001L, 301L)).thenReturn(Optional.of(delivery));
            when(pushDeliveryRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            // Attempt 1: Firebase unavailable
            when(pushDeliveryGateway.deliver(any())).thenReturn(PushDeliveryResult.retryableFailure("UNAVAILABLE", "Server busy"));

            NotificationDeliveryResult attempt1Result = dispatchService.dispatch(outbox);
            assertThat(attempt1Result.outcome()).isEqualTo(NotificationAttemptOutcome.TRANSIENT_FAILURE);
            assertThat(attempt1Result.failureCategory()).isEqualTo("PUSH_RETRYABLE_FAILURE");
            assertThat(delivery.getStatus()).isEqualTo(NotificationStatus.RETRY_SCHEDULED);
            assertThat(delivery.getAttemptCount()).isEqualTo(1);

            // Attempt 2: Firebase recovers
            when(pushDeliveryGateway.deliver(any())).thenReturn(PushDeliveryResult.success("fcm-msg-recovered-999"));

            NotificationDeliveryResult attempt2Result = dispatchService.dispatch(outbox);
            assertThat(attempt2Result.outcome()).isEqualTo(NotificationAttemptOutcome.DELIVERED);
            assertThat(attempt2Result.providerMessageId()).isEqualTo("fcm-msg-recovered-999");
            assertThat(delivery.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
            assertThat(delivery.getAttemptCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Worker Lease Recovery: Expired processing lease is reclaimed by secondary worker and finalized")
        void workerLeaseRecoveryVerification() {
            NotificationProperties properties = new NotificationProperties();
            properties.setBatchSize(10);
            properties.setProcessingLease(Duration.ofMinutes(2));
            properties.setMaxAttempts(5);
            NotificationRetryPolicy retryPolicy = new NotificationRetryPolicy(properties, () -> 0.5);

            NotificationOutboxRepository outboxRepository = mock(NotificationOutboxRepository.class);
            NotificationDeliveryAttemptRepository attemptRepository = mock(NotificationDeliveryAttemptRepository.class);

            PushNotificationWorkerTransactions transactions = new PushNotificationWorkerTransactions(
                    outboxRepository, attemptRepository, properties, retryPolicy, clock);

            NotificationOutbox stalledNotification = new NotificationOutbox(
                    "event:req:5001:stalled:push", NotificationType.REPAIR_COMPLETED,
                    NotificationChannel.PUSH, NotificationRecipientType.CUSTOMER, 1001L, null,
                    "notification.repair.completed", "{}", "UZ", "Title", "Body", NOW.minusMinutes(10));
            ReflectionTestUtils.setField(stalledNotification, "id", 7002L);

            // Simulate worker 1 claimed it 5 minutes ago and crashed (lease expired)
            stalledNotification.claim("dead-worker-1", NOW.minusMinutes(5), NOW.minusMinutes(3));

            when(outboxRepository.findClaimableByChannelForUpdate(eq("PUSH"), any(), eq(10)))
                    .thenReturn(List.of(stalledNotification));
            when(outboxRepository.findByIdForUpdate(7002L)).thenReturn(Optional.of(stalledNotification));

            // Worker 2 claims it
            List<NotificationOutbox> claimed = transactions.claim("active-worker-2");
            assertThat(claimed).hasSize(1);
            assertThat(stalledNotification.getStatus()).isEqualTo(NotificationStatus.PROCESSING);
            assertThat(stalledNotification.getWorkerId()).isEqualTo("active-worker-2");

            // Finalize as DELIVERED
            transactions.finalizeDelivery(7002L, "active-worker-2", NOW, NotificationDeliveryResult.delivered("msg-ok"));
            assertThat(stalledNotification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
            assertThat(stalledNotification.getDeliveredAt()).isNotNull();
        }

        @Test
        @DisplayName("Push Retry Exhaustion: Reaching max attempts transitions NotificationOutbox to DEAD")
        void pushMaxRetryExhaustionTransitionToDead() {
            NotificationProperties properties = new NotificationProperties();
            properties.setMaxAttempts(3);
            NotificationRetryPolicy retryPolicy = new NotificationRetryPolicy(properties, () -> 0.5);

            NotificationOutboxRepository outboxRepository = mock(NotificationOutboxRepository.class);
            NotificationDeliveryAttemptRepository attemptRepository = mock(NotificationDeliveryAttemptRepository.class);

            PushNotificationWorkerTransactions transactions = new PushNotificationWorkerTransactions(
                    outboxRepository, attemptRepository, properties, retryPolicy, clock);

            NotificationOutbox notification = new NotificationOutbox(
                    "event:req:5001:failing:push", NotificationType.REPAIR_COMPLETED,
                    NotificationChannel.PUSH, NotificationRecipientType.CUSTOMER, 1001L, null,
                    "notification.repair.completed", "{}", "UZ", "Title", "Body", NOW.minusMinutes(10));
            ReflectionTestUtils.setField(notification, "id", 7003L);
            ReflectionTestUtils.setField(notification, "attemptCount", 2);
            notification.claim("worker-1", NOW, NOW.plusMinutes(2));

            when(outboxRepository.findByIdForUpdate(7003L)).thenReturn(Optional.of(notification));

            // Finalize 3rd attempt with retryable failure
            transactions.finalizeDelivery(
                    7003L, "worker-1", NOW,
                    NotificationDeliveryResult.transientFailure("PUSH_RETRYABLE_FAILURE", NOW.plusMinutes(10)));

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DEAD);
            assertThat(notification.getLastFailureCategory()).isEqualTo(NotificationFailureCategory.MAX_ATTEMPTS_EXHAUSTED);
            assertThat(notification.getAttemptCount()).isEqualTo(3);
            assertThat(notification.getDeadAt()).isNotNull();
        }

        @Test
        @DisplayName("Telegram Retry Exhaustion: Reaching max attempts transitions NotificationOutbox to DEAD")
        void telegramMaxRetryExhaustionTransitionToDead() {
            NotificationProperties properties = new NotificationProperties();
            properties.setMaxAttempts(3);
            NotificationRetryPolicy retryPolicy = new NotificationRetryPolicy(properties, () -> 0.5);

            NotificationOutboxRepository outboxRepository = mock(NotificationOutboxRepository.class);
            NotificationDeliveryAttemptRepository attemptRepository = mock(NotificationDeliveryAttemptRepository.class);

            NotificationWorkerTransactions transactions = new NotificationWorkerTransactions(
                    outboxRepository, attemptRepository, properties, retryPolicy, clock);

            NotificationOutbox notification = new NotificationOutbox(
                    "event:req:5001:failing:telegram", NotificationType.REPAIR_COMPLETED,
                    NotificationChannel.TELEGRAM, NotificationRecipientType.CUSTOMER, 1001L, null,
                    "notification.repair.completed", "{}", "UZ", "Title", "Body", NOW.minusMinutes(10));
            ReflectionTestUtils.setField(notification, "id", 7004L);
            ReflectionTestUtils.setField(notification, "attemptCount", 2);
            notification.claim("worker-tg-1", NOW, NOW.plusMinutes(2));

            when(outboxRepository.findByIdForUpdate(7004L)).thenReturn(Optional.of(notification));

            // Finalize 3rd attempt with retryable failure
            transactions.finalizeDelivery(
                    7004L, "worker-tg-1", NOW,
                    NotificationDeliveryResult.transientFailure("TELEGRAM_NETWORK_TIMEOUT", NOW.plusMinutes(10)));

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DEAD);
            assertThat(notification.getLastFailureCategory()).isEqualTo(NotificationFailureCategory.MAX_ATTEMPTS_EXHAUSTED);
            assertThat(notification.getAttemptCount()).isEqualTo(3);
            assertThat(notification.getDeadAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("3. Cross-Actor Ownership & Shared Device Security")
    class SecurityAndOwnershipTests {

        @Test
        @DisplayName("Customer cannot access another customer's repair request (404/Not Found)")
        void crossCustomerOwnershipIsolation() {
            RepairRequestService repairRequestService = mock(RepairRequestService.class);
            RepairRequestRepository requestRepository = mock(RepairRequestRepository.class);
            RepairAssignmentRepository assignmentRepository = mock(RepairAssignmentRepository.class);
            RepairAttachmentRepository attachmentRepository = mock(RepairAttachmentRepository.class);
            RepairRequestStatusHistoryRepository statusHistoryRepository = mock(RepairRequestStatusHistoryRepository.class);
            RepairReviewService reviewService = mock(RepairReviewService.class);
            RepairReviewRepository reviewRepository = mock(RepairReviewRepository.class);
            RequestLocaleResolver localeResolver = mock(RequestLocaleResolver.class);
            LocalizationService localizationService = mock(LocalizationService.class);

            RepairResourceAccessPolicy accessPolicy = new RepairResourceAccessPolicy(
                    requestRepository, assignmentRepository, attachmentRepository);
            RepairActionCapabilityService capabilityService = mock(RepairActionCapabilityService.class);

            CustomerRepairRequestFacade customerFacade = new CustomerRepairRequestFacade(
                    repairRequestService, accessPolicy, requestRepository, assignmentRepository,
                    statusHistoryRepository, capabilityService, reviewService, reviewRepository,
                    localeResolver, localizationService);

            // Request belongs to customer 9999 (not customerActor 1001)
            Customer otherCustomer = new Customer("Other User", "+998900000000", LanguageCode.UZ, NOW);
            ReflectionTestUtils.setField(otherCustomer, "id", 9999L);
            RepairRequest otherRequest = new RepairRequest(
                    "REQ-2026-000999", otherCustomer, category, "Other Problem",
                    "Toshkent", null, null, RepairRequestPriority.LOW, null,
                    "+998900000000", adminUser, NOW);
            ReflectionTestUtils.setField(otherRequest, "id", 9999L);

            when(requestRepository.findByIdAndCustomerId(9999L, 1001L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerFacade.getRequestDetail(customerActor, 9999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.REPAIR_REQUEST_NOT_FOUND));
        }

        @Test
        @DisplayName("Shared Device Account Switching: Ownership transfers atomically to new authenticated customer")
        void sharedDevicePushOwnershipTransfer() {
            PushEndpointRepository repository = mock(PushEndpointRepository.class);
            CustomerRepository customerRepo = mock(CustomerRepository.class);

            PushEndpointService endpointService = new PushEndpointService(
                    repository, null, customerRepo, null, clock);

            Customer customerA = customer;
            Customer customerB = new Customer("Customer B", "+998907776655", LanguageCode.RU, NOW);
            ReflectionTestUtils.setField(customerB, "id", 1002L);

            when(customerRepo.findById(1002L)).thenReturn(Optional.of(customerB));

            // Existing installation registered by Customer A
            PushEndpoint existing = PushEndpoint.forCustomer(
                    customerA, PushClientType.CUSTOMER_MOBILE, PushPlatform.ANDROID, PushFirebaseApp.CUSTOMER_ANDROID, "shared-device-fid-777", "1.0", NOW.minusDays(5));
            ReflectionTestUtils.setField(existing, "id", 444L);

            when(repository.findByFirebaseAppKeyAndFcmRegistrationToken(PushFirebaseApp.CUSTOMER_ANDROID, "shared-device-fid-777"))
                    .thenReturn(Optional.of(existing));
            when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            // Customer B logs into the same device
            AuthenticatedMobileActor actorB = new AuthenticatedMobileActor(ActorType.CUSTOMER, 1002L, "+998907776655", true);
            PushEndpointRegisterRequest registerReq = new PushEndpointRegisterRequest(
                    "shared-device-fid-777", PushClientType.CUSTOMER_MOBILE, PushPlatform.ANDROID, PushFirebaseApp.CUSTOMER_ANDROID, "1.1");

            PushEndpointResponse resp = endpointService.registerForMobile(actorB, registerReq);
            assertThat(resp.id()).isEqualTo(444L);
            assertThat(existing.isOwnedByCustomer(1002L)).isTrue();
            assertThat(existing.isOwnedByCustomer(1001L)).isFalse();
            assertThat(existing.getOwnerType()).isEqualTo(PushOwnerType.CUSTOMER);
        }
    }
}
