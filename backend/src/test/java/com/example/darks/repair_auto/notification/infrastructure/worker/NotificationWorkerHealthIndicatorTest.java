package com.example.darks.repair_auto.notification.infrastructure.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.notification.domain.NotificationChannel;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationOutboxRepository;
import com.example.darks.repair_auto.notification.push.infrastructure.PushEndpointRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

class NotificationWorkerHealthIndicatorTest {

    @Test
    void givenWorkerRepositoryCounts_whenHealth_thenReturnsUpWithAccurateQueueStatistics() {
        NotificationOutboxRepository outboxRepository = mock(NotificationOutboxRepository.class);
        PushEndpointRepository pushEndpointRepository = mock(PushEndpointRepository.class);
        NotificationProperties properties = new NotificationProperties();

        when(outboxRepository.countByChannelAndStatus(NotificationChannel.TELEGRAM, NotificationStatus.PENDING)).thenReturn(5L);
        when(outboxRepository.countByChannelAndStatus(NotificationChannel.TELEGRAM, NotificationStatus.PROCESSING)).thenReturn(1L);
        when(outboxRepository.countByChannelAndStatus(NotificationChannel.TELEGRAM, NotificationStatus.RETRY_SCHEDULED)).thenReturn(2L);
        when(outboxRepository.countByChannelAndStatus(NotificationChannel.TELEGRAM, NotificationStatus.DEAD)).thenReturn(0L);

        when(outboxRepository.countByChannelAndStatus(NotificationChannel.PUSH, NotificationStatus.PENDING)).thenReturn(10L);
        when(outboxRepository.countByChannelAndStatus(NotificationChannel.PUSH, NotificationStatus.PROCESSING)).thenReturn(2L);
        when(outboxRepository.countByChannelAndStatus(NotificationChannel.PUSH, NotificationStatus.RETRY_SCHEDULED)).thenReturn(3L);
        when(outboxRepository.countByChannelAndStatus(NotificationChannel.PUSH, NotificationStatus.DEAD)).thenReturn(1L);

        when(pushEndpointRepository.countByEnabledTrue()).thenReturn(50L);
        when(pushEndpointRepository.countByEnabledFalse()).thenReturn(12L);

        NotificationWorkerHealthIndicator indicator = new NotificationWorkerHealthIndicator(
                outboxRepository, pushEndpointRepository, properties);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("workerEnabled", true);

        @SuppressWarnings("unchecked")
        Map<String, Object> telegramQueue = (Map<String, Object>) health.getDetails().get("telegramQueue");
        assertThat(telegramQueue).containsEntry("pending", 5L);
        assertThat(telegramQueue).containsEntry("processing", 1L);
        assertThat(telegramQueue).containsEntry("retryScheduled", 2L);
        assertThat(telegramQueue).containsEntry("dead", 0L);

        @SuppressWarnings("unchecked")
        Map<String, Object> pushQueue = (Map<String, Object>) health.getDetails().get("pushQueue");
        assertThat(pushQueue).containsEntry("pending", 10L);
        assertThat(pushQueue).containsEntry("processing", 2L);
        assertThat(pushQueue).containsEntry("retryScheduled", 3L);
        assertThat(pushQueue).containsEntry("dead", 1L);

        @SuppressWarnings("unchecked")
        Map<String, Object> endpoints = (Map<String, Object>) health.getDetails().get("endpoints");
        assertThat(endpoints).containsEntry("active", 50L);
        assertThat(endpoints).containsEntry("disabled", 12L);
    }
}
