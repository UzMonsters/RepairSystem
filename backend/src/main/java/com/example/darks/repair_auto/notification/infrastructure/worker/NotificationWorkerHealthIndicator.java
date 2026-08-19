package com.example.darks.repair_auto.notification.infrastructure.worker;

import com.example.darks.repair_auto.notification.domain.NotificationChannel;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationOutboxRepository;
import com.example.darks.repair_auto.notification.push.infrastructure.PushEndpointRepository;
import java.util.Map;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("notificationWorkerHealthIndicator")
public class NotificationWorkerHealthIndicator implements HealthIndicator {

    private final NotificationOutboxRepository outboxRepository;
    private final PushEndpointRepository pushEndpointRepository;
    private final NotificationProperties properties;

    public NotificationWorkerHealthIndicator(
            NotificationOutboxRepository outboxRepository,
            PushEndpointRepository pushEndpointRepository,
            NotificationProperties properties) {
        this.outboxRepository = outboxRepository;
        this.pushEndpointRepository = pushEndpointRepository;
        this.properties = properties;
    }

    @Override
    public Health health() {
        boolean workerEnabled = properties.isWorkerEnabled();

        long telegramPending = outboxRepository.countByChannelAndStatus(NotificationChannel.TELEGRAM, NotificationStatus.PENDING);
        long telegramProcessing = outboxRepository.countByChannelAndStatus(NotificationChannel.TELEGRAM, NotificationStatus.PROCESSING);
        long telegramRetry = outboxRepository.countByChannelAndStatus(NotificationChannel.TELEGRAM, NotificationStatus.RETRY_SCHEDULED);
        long telegramDead = outboxRepository.countByChannelAndStatus(NotificationChannel.TELEGRAM, NotificationStatus.DEAD);

        long pushPending = outboxRepository.countByChannelAndStatus(NotificationChannel.PUSH, NotificationStatus.PENDING);
        long pushProcessing = outboxRepository.countByChannelAndStatus(NotificationChannel.PUSH, NotificationStatus.PROCESSING);
        long pushRetry = outboxRepository.countByChannelAndStatus(NotificationChannel.PUSH, NotificationStatus.RETRY_SCHEDULED);
        long pushDead = outboxRepository.countByChannelAndStatus(NotificationChannel.PUSH, NotificationStatus.DEAD);

        long activePushEndpoints = pushEndpointRepository.countByEnabledTrue();
        long disabledPushEndpoints = pushEndpointRepository.countByEnabledFalse();

        Map<String, Object> telegramQueue = Map.of(
                "pending", telegramPending,
                "processing", telegramProcessing,
                "retryScheduled", telegramRetry,
                "dead", telegramDead
        );

        Map<String, Object> pushQueue = Map.of(
                "pending", pushPending,
                "processing", pushProcessing,
                "retryScheduled", pushRetry,
                "dead", pushDead
        );

        Map<String, Object> endpointStats = Map.of(
                "active", activePushEndpoints,
                "disabled", disabledPushEndpoints
        );

        return Health.up()
                .withDetail("workerEnabled", workerEnabled)
                .withDetail("telegramQueue", telegramQueue)
                .withDetail("pushQueue", pushQueue)
                .withDetail("endpoints", endpointStats)
                .build();
    }
}
