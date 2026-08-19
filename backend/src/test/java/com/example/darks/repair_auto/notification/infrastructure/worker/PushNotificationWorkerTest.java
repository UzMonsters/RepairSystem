package com.example.darks.repair_auto.notification.infrastructure.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.notification.application.NotificationDeliveryResult;
import com.example.darks.repair_auto.notification.application.PushNotificationDispatchService;
import com.example.darks.repair_auto.notification.domain.NotificationChannel;
import com.example.darks.repair_auto.notification.domain.NotificationOutbox;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PushNotificationWorkerTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private PushNotificationWorkerTransactions transactions;
    private PushNotificationDispatchService dispatchService;
    private NotificationProperties properties;
    private SimpleMeterRegistry meterRegistry;
    private Clock clock;
    private PushNotificationWorker worker;

    @BeforeEach
    void setUp() {
        transactions = mock(PushNotificationWorkerTransactions.class);
        dispatchService = mock(PushNotificationDispatchService.class);
        properties = new NotificationProperties();
        properties.setWorkerEnabled(true);
        meterRegistry = new SimpleMeterRegistry();
        clock = Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);

        worker = new PushNotificationWorker(transactions, dispatchService, properties, clock, meterRegistry);
    }

    @Test
    void givenClaimedPushNotification_whenRunOnce_thenDispatchesAndFinalizes() {
        NotificationOutbox outbox = new NotificationOutbox(
                "req:101:status:COMPLETED:customer:42:push",
                NotificationType.REPAIR_COMPLETED,
                NotificationChannel.PUSH,
                NotificationRecipientType.CUSTOMER,
                42L,
                null,
                "notification.repair.completed",
                "{}",
                "UZ",
                "Title",
                "Message",
                NOW);
        ReflectionTestUtils.setField(outbox, "id", 701L);

        when(transactions.claim(any())).thenReturn(List.of(outbox));
        when(dispatchService.dispatch(outbox)).thenReturn(NotificationDeliveryResult.delivered());

        int processed = worker.runOnce();

        assertThat(processed).isEqualTo(1);
        verify(transactions).finalizeDelivery(eq(701L), any(), eq(NOW), any(NotificationDeliveryResult.class));
    }
}
