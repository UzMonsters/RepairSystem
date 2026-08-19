package com.example.darks.repair_auto.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.notification.domain.NotificationAttemptOutcome;
import com.example.darks.repair_auto.notification.domain.NotificationChannel;
import com.example.darks.repair_auto.notification.domain.NotificationOutbox;
import com.example.darks.repair_auto.notification.domain.NotificationPushDelivery;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationPushDeliveryRepository;
import com.example.darks.repair_auto.notification.infrastructure.worker.NotificationProperties;
import com.example.darks.repair_auto.notification.push.application.PushEndpointService;
import com.example.darks.repair_auto.notification.push.config.FirebasePushProperties;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.notification.push.domain.PushEndpoint;
import com.example.darks.repair_auto.notification.push.domain.PushFirebaseApp;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import com.example.darks.repair_auto.notification.push.gateway.PushDeliveryCommand;
import com.example.darks.repair_auto.notification.push.gateway.PushDeliveryGateway;
import com.example.darks.repair_auto.notification.push.gateway.PushDeliveryResult;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class PushNotificationDispatchServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private PushEndpointService pushEndpointService;
    private PushDeliveryGateway pushDeliveryGateway;
    private NotificationPushDeliveryRepository pushDeliveryRepository;
    private NotificationRetryPolicy retryPolicy;
    private FirebasePushProperties firebasePushProperties;
    private Clock clock;
    private PushNotificationDispatchService dispatchService;

    private Customer customer;
    private PushEndpoint endpoint1;
    private PushEndpoint endpoint2;
    private NotificationOutbox notificationOutbox;

    @BeforeEach
    void setUp() {
        pushEndpointService = mock(PushEndpointService.class);
        pushDeliveryGateway = mock(PushDeliveryGateway.class);
        pushDeliveryRepository = mock(NotificationPushDeliveryRepository.class);
        NotificationProperties notifProps = new NotificationProperties();
        retryPolicy = new NotificationRetryPolicy(notifProps);
        firebasePushProperties = new FirebasePushProperties(true, "repairauto-dev", null, Duration.ofSeconds(10), Duration.ofSeconds(10));
        clock = Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);

        dispatchService = new PushNotificationDispatchService(
                pushEndpointService,
                pushDeliveryGateway,
                pushDeliveryRepository,
                retryPolicy,
                firebasePushProperties,
                clock);

        customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(customer, "id", 42L);

        endpoint1 = PushEndpoint.forCustomer(
                customer,
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                "fid-android-1",
                "1.0.0",
                NOW);
        ReflectionTestUtils.setField(endpoint1, "id", 101L);

        endpoint2 = PushEndpoint.forCustomer(
                customer,
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.IOS,
                PushFirebaseApp.CUSTOMER_IOS,
                "fid-ios-2",
                "1.0.0",
                NOW);
        ReflectionTestUtils.setField(endpoint2, "id", 102L);

        notificationOutbox = new NotificationOutbox(
                "req:101:status:COMPLETED:customer:42:push",
                NotificationType.REPAIR_COMPLETED,
                NotificationChannel.PUSH,
                NotificationRecipientType.CUSTOMER,
                42L,
                null,
                "notification.repair.completed",
                "{\"requestId\":\"101\",\"requestNumber\":\"REQ-2026-000042\"}",
                "UZ",
                "Ta'mirlash yakunlandi",
                "REQ-2026-000042 yakunlandi",
                NOW);
        ReflectionTestUtils.setField(notificationOutbox, "id", 501L);
    }

    @Test
    void givenFirebaseDisabled_whenDispatch_thenReturnsUnavailableWithPushDisabled() {
        FirebasePushProperties disabledProps = new FirebasePushProperties(false, "repairauto-dev", null, Duration.ofSeconds(10), Duration.ofSeconds(10));
        PushNotificationDispatchService service = new PushNotificationDispatchService(
                pushEndpointService,
                pushDeliveryGateway,
                pushDeliveryRepository,
                retryPolicy,
                disabledProps,
                clock);

        NotificationDeliveryResult result = service.dispatch(notificationOutbox);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.RECIPIENT_UNAVAILABLE);
        assertThat(result.failureCategory()).isEqualTo("PUSH_DISABLED");
        verify(pushDeliveryGateway, never()).deliver(any());
    }

    @Test
    void givenNoActiveEndpoints_whenDispatch_thenReturnsUnavailableWithNoActiveEndpoints() {
        when(pushEndpointService.findEnabledForCustomer(42L)).thenReturn(List.of());
        when(pushDeliveryRepository.findByNotificationOutboxId(501L)).thenReturn(List.of());

        NotificationDeliveryResult result = dispatchService.dispatch(notificationOutbox);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.RECIPIENT_UNAVAILABLE);
        assertThat(result.failureCategory()).isEqualTo("NO_ACTIVE_ENDPOINTS");
        verify(pushDeliveryGateway, never()).deliver(any());
    }

    @Test
    void givenSingleEndpoint_whenDeliverySucceeds_thenReturnsDelivered() {
        when(pushEndpointService.findEnabledForCustomer(42L)).thenReturn(List.of(endpoint1));

        NotificationPushDelivery createdDelivery = new NotificationPushDelivery(notificationOutbox, endpoint1, NOW);
        when(pushDeliveryRepository.findByNotificationOutboxIdAndPushEndpointId(501L, 101L))
                .thenReturn(Optional.of(createdDelivery));
        when(pushDeliveryGateway.deliver(any(PushDeliveryCommand.class)))
                .thenReturn(PushDeliveryResult.success("msg-fcm-123"));

        NotificationDeliveryResult result = dispatchService.dispatch(notificationOutbox);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.DELIVERED);
        assertThat(result.providerMessageId()).isEqualTo("msg-fcm-123");
        assertThat(createdDelivery.getAttemptCount()).isEqualTo(1);
        assertThat(createdDelivery.getFirebaseMessageId()).isEqualTo("msg-fcm-123");
        verify(pushDeliveryRepository).saveAndFlush(createdDelivery);
    }

    @Test
    void givenMultipleEndpoints_whenAllSucceed_thenReturnsDeliveredAndPopulatesSemanticData() {
        when(pushEndpointService.findEnabledForCustomer(42L)).thenReturn(List.of(endpoint1, endpoint2));

        NotificationPushDelivery d1 = new NotificationPushDelivery(notificationOutbox, endpoint1, NOW);
        NotificationPushDelivery d2 = new NotificationPushDelivery(notificationOutbox, endpoint2, NOW);
        when(pushDeliveryRepository.findByNotificationOutboxIdAndPushEndpointId(501L, 101L))
                .thenReturn(Optional.of(d1));
        when(pushDeliveryRepository.findByNotificationOutboxIdAndPushEndpointId(501L, 102L))
                .thenReturn(Optional.of(d2));

        when(pushDeliveryGateway.deliver(any(PushDeliveryCommand.class))).thenAnswer(inv -> {
            PushDeliveryCommand cmd = inv.getArgument(0);
            if (cmd.endpoint().getId().equals(101L)) {
                return PushDeliveryResult.success("msg-1");
            } else {
                return PushDeliveryResult.success("msg-2");
            }
        });

        NotificationDeliveryResult result = dispatchService.dispatch(notificationOutbox);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.DELIVERED);
        assertThat(d1.getAttemptCount()).isEqualTo(1);
        assertThat(d2.getAttemptCount()).isEqualTo(1);

        ArgumentCaptor<PushDeliveryCommand> captor = ArgumentCaptor.forClass(PushDeliveryCommand.class);
        verify(pushDeliveryGateway, times(2)).deliver(captor.capture());

        List<PushDeliveryCommand> commands = captor.getAllValues();
        assertThat(commands.get(0).additionalData()).containsEntry("target", "REPAIR_REQUEST_DETAILS");
        assertThat(commands.get(0).additionalData()).containsEntry("repairRequestId", "101");
        assertThat(commands.get(0).additionalData()).containsEntry("requestNumber", "REQ-2026-000042");
    }

    @Test
    void givenOneSuccessAndOneRetryable_whenDispatch_thenReturnsTransientFailureForParent() {
        when(pushEndpointService.findEnabledForCustomer(42L)).thenReturn(List.of(endpoint1, endpoint2));

        NotificationPushDelivery d1 = new NotificationPushDelivery(notificationOutbox, endpoint1, NOW);
        NotificationPushDelivery d2 = new NotificationPushDelivery(notificationOutbox, endpoint2, NOW);
        when(pushDeliveryRepository.findByNotificationOutboxIdAndPushEndpointId(501L, 101L))
                .thenReturn(Optional.of(d1));
        when(pushDeliveryRepository.findByNotificationOutboxIdAndPushEndpointId(501L, 102L))
                .thenReturn(Optional.of(d2));

        when(pushDeliveryGateway.deliver(any(PushDeliveryCommand.class))).thenAnswer(inv -> {
            PushDeliveryCommand cmd = inv.getArgument(0);
            if (cmd.endpoint().getId().equals(101L)) {
                return PushDeliveryResult.success("msg-1");
            } else {
                return PushDeliveryResult.retryableFailure("UNAVAILABLE", "Busy");
            }
        });

        NotificationDeliveryResult result = dispatchService.dispatch(notificationOutbox);

        // Core Invariant: DELIVERED + RETRY_SCHEDULED => parent RETRY_SCHEDULED (TRANSIENT_FAILURE)
        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.TRANSIENT_FAILURE);
        assertThat(result.failureCategory()).isEqualTo("PUSH_RETRYABLE_FAILURE");
        assertThat(result.nextAttemptAt()).isNotNull();
        assertThat(d1.getAttemptCount()).isEqualTo(1);
        assertThat(d2.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void givenOneSuccessAndOneUnregistered_whenDispatch_thenDisablesDeadEndpointAndReturnsDelivered() {
        when(pushEndpointService.findEnabledForCustomer(42L)).thenReturn(List.of(endpoint1, endpoint2));

        NotificationPushDelivery d1 = new NotificationPushDelivery(notificationOutbox, endpoint1, NOW);
        NotificationPushDelivery d2 = new NotificationPushDelivery(notificationOutbox, endpoint2, NOW);
        when(pushDeliveryRepository.findByNotificationOutboxIdAndPushEndpointId(501L, 101L))
                .thenReturn(Optional.of(d1));
        when(pushDeliveryRepository.findByNotificationOutboxIdAndPushEndpointId(501L, 102L))
                .thenReturn(Optional.of(d2));

        when(pushDeliveryGateway.deliver(any(PushDeliveryCommand.class))).thenAnswer(inv -> {
            PushDeliveryCommand cmd = inv.getArgument(0);
            if (cmd.endpoint().getId().equals(101L)) {
                return PushDeliveryResult.success("msg-1");
            } else {
                return PushDeliveryResult.permanentFailure("UNREGISTERED", "Target not found");
            }
        });

        NotificationDeliveryResult result = dispatchService.dispatch(notificationOutbox);

        // DELIVERED + DEAD => parent DELIVERED
        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.DELIVERED);
        verify(pushEndpointService).disableInvalidEndpoint(102L);
        assertThat(d1.getAttemptCount()).isEqualTo(1);
        assertThat(d2.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void givenAlreadyDeliveredEndpoint_whenRetryOccurs_thenDoesNotResendToDeliveredEndpointAndDeliversRemaining() {
        NotificationPushDelivery deliveredAttempt = new NotificationPushDelivery(notificationOutbox, endpoint1, NOW.minusMinutes(5));
        deliveredAttempt.markDelivered(NOW.minusMinutes(5), "msg-prev");

        NotificationPushDelivery retryAttempt = new NotificationPushDelivery(notificationOutbox, endpoint2, NOW.minusMinutes(5));
        retryAttempt.markRetry(NOW.minusMinutes(5), NOW.minusSeconds(1), "UNAVAILABLE", "RETRYABLE");

        when(pushEndpointService.findEnabledForCustomer(42L)).thenReturn(List.of(endpoint1, endpoint2));
        when(pushDeliveryRepository.findByNotificationOutboxIdAndPushEndpointId(501L, 101L))
                .thenReturn(Optional.of(deliveredAttempt));
        when(pushDeliveryRepository.findByNotificationOutboxIdAndPushEndpointId(501L, 102L))
                .thenReturn(Optional.of(retryAttempt));

        when(pushDeliveryGateway.deliver(any(PushDeliveryCommand.class))).thenReturn(PushDeliveryResult.success("msg-new-ios"));

        NotificationDeliveryResult result = dispatchService.dispatch(notificationOutbox);

        // DELIVERED + DELIVERED => parent DELIVERED
        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.DELIVERED);
        // Only endpoint 2 was called (endpoint 1 was skipped!)
        verify(pushDeliveryGateway, times(1)).deliver(any());
        assertThat(deliveredAttempt.getAttemptCount()).isEqualTo(1);
        assertThat(deliveredAttempt.getFirebaseMessageId()).isEqualTo("msg-prev");
        assertThat(retryAttempt.getAttemptCount()).isEqualTo(2);
    }

    @Test
    void givenDeadAndRetryableEndpoints_whenDispatch_thenReturnsTransientFailure() {
        NotificationPushDelivery deadAttempt = new NotificationPushDelivery(notificationOutbox, endpoint1, NOW);
        deadAttempt.markDead(NOW, "INVALID_PAYLOAD", "INVALID_PAYLOAD");

        NotificationPushDelivery retryAttempt = new NotificationPushDelivery(notificationOutbox, endpoint2, NOW);

        when(pushEndpointService.findEnabledForCustomer(42L)).thenReturn(List.of(endpoint1, endpoint2));
        when(pushDeliveryRepository.findByNotificationOutboxIdAndPushEndpointId(501L, 101L))
                .thenReturn(Optional.of(deadAttempt));
        when(pushDeliveryRepository.findByNotificationOutboxIdAndPushEndpointId(501L, 102L))
                .thenReturn(Optional.of(retryAttempt));

        when(pushDeliveryGateway.deliver(any(PushDeliveryCommand.class))).thenReturn(PushDeliveryResult.retryableFailure("INTERNAL", "Server error"));

        NotificationDeliveryResult result = dispatchService.dispatch(notificationOutbox);

        // DEAD + RETRY_SCHEDULED => parent RETRY_SCHEDULED
        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.TRANSIENT_FAILURE);
        assertThat(result.failureCategory()).isEqualTo("PUSH_RETRYABLE_FAILURE");
        assertThat(deadAttempt.getAttemptCount()).isEqualTo(1);
        assertThat(retryAttempt.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void givenAllDeadEndpoints_whenDispatch_thenReturnsPermanentFailure() {
        NotificationPushDelivery d1 = new NotificationPushDelivery(notificationOutbox, endpoint1, NOW);
        NotificationPushDelivery d2 = new NotificationPushDelivery(notificationOutbox, endpoint2, NOW);

        when(pushEndpointService.findEnabledForCustomer(42L)).thenReturn(List.of(endpoint1, endpoint2));
        when(pushDeliveryRepository.findByNotificationOutboxIdAndPushEndpointId(501L, 101L))
                .thenReturn(Optional.of(d1));
        when(pushDeliveryRepository.findByNotificationOutboxIdAndPushEndpointId(501L, 102L))
                .thenReturn(Optional.of(d2));

        when(pushDeliveryGateway.deliver(any(PushDeliveryCommand.class))).thenAnswer(inv -> {
            PushDeliveryCommand cmd = inv.getArgument(0);
            if (cmd.endpoint().getId().equals(101L)) {
                return PushDeliveryResult.permanentFailure("UNREGISTERED", "Gone");
            } else {
                return PushDeliveryResult.invalidPayload("INVALID_PAYLOAD", "Bad json");
            }
        });

        NotificationDeliveryResult result = dispatchService.dispatch(notificationOutbox);

        // DEAD + DEAD => parent PERMANENT_FAILURE (DEAD)
        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.PERMANENT_FAILURE);
        assertThat(result.failureCategory()).isEqualTo("ALL_ENDPOINTS_FAILED");
    }

    @Test
    void givenAggregatePushDeliveryState_whenEvaluatedDirectly_thenMatchesAllRules() {
        NotificationPushDelivery delivered = new NotificationPushDelivery(notificationOutbox, endpoint1, NOW);
        delivered.markDelivered(NOW, "msg-1");

        NotificationPushDelivery retry = new NotificationPushDelivery(notificationOutbox, endpoint2, NOW);
        retry.markRetry(NOW, NOW.plusSeconds(30), "UNAVAILABLE", "RETRY");

        NotificationPushDelivery dead = new NotificationPushDelivery(notificationOutbox, endpoint1, NOW);
        dead.markDead(NOW, "UNREGISTERED", "UNREGISTERED");

        // 1. DELIVERED + DELIVERED => DELIVERED
        assertThat(dispatchService.aggregatePushDeliveryState(List.of(delivered, delivered), NOW, 1).outcome())
                .isEqualTo(NotificationAttemptOutcome.DELIVERED);

        // 2. DELIVERED + RETRY_SCHEDULED => RETRY_SCHEDULED (TRANSIENT_FAILURE)
        assertThat(dispatchService.aggregatePushDeliveryState(List.of(delivered, retry), NOW, 1).outcome())
                .isEqualTo(NotificationAttemptOutcome.TRANSIENT_FAILURE);

        // 3. RETRY_SCHEDULED + RETRY_SCHEDULED => RETRY_SCHEDULED (TRANSIENT_FAILURE)
        assertThat(dispatchService.aggregatePushDeliveryState(List.of(retry, retry), NOW, 1).outcome())
                .isEqualTo(NotificationAttemptOutcome.TRANSIENT_FAILURE);

        // 4. DELIVERED + DEAD => DELIVERED
        assertThat(dispatchService.aggregatePushDeliveryState(List.of(delivered, dead), NOW, 1).outcome())
                .isEqualTo(NotificationAttemptOutcome.DELIVERED);

        // 5. DEAD + RETRY_SCHEDULED => RETRY_SCHEDULED (TRANSIENT_FAILURE)
        assertThat(dispatchService.aggregatePushDeliveryState(List.of(dead, retry), NOW, 1).outcome())
                .isEqualTo(NotificationAttemptOutcome.TRANSIENT_FAILURE);

        // 6. DEAD + DEAD => PERMANENT_FAILURE
        assertThat(dispatchService.aggregatePushDeliveryState(List.of(dead, dead), NOW, 1).outcome())
                .isEqualTo(NotificationAttemptOutcome.PERMANENT_FAILURE);

        // 7. Empty list => RECIPIENT_UNAVAILABLE
        assertThat(dispatchService.aggregatePushDeliveryState(List.of(), NOW, 1).outcome())
                .isEqualTo(NotificationAttemptOutcome.RECIPIENT_UNAVAILABLE);
    }
}
