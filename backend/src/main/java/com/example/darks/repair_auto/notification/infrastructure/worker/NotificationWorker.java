package com.example.darks.repair_auto.notification.infrastructure.worker;

import com.example.darks.repair_auto.notification.application.NotificationDeliveryService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationWorker.class);

    private final NotificationWorkerTransactions transactions;
    private final NotificationDeliveryService deliveryService;
    private final NotificationProperties properties;
    private final Clock clock;
    private final MeterRegistry meterRegistry;
    private final String workerId = "notification-worker-" + UUID.randomUUID();

    @Autowired
    public NotificationWorker(
            NotificationWorkerTransactions transactions,
            NotificationDeliveryService deliveryService,
            NotificationProperties properties,
            MeterRegistry meterRegistry) {
        this(transactions, deliveryService, properties, Clock.systemUTC(), meterRegistry);
    }

    NotificationWorker(
            NotificationWorkerTransactions transactions,
            NotificationDeliveryService deliveryService,
            NotificationProperties properties,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.transactions = transactions;
        this.deliveryService = deliveryService;
        this.properties = properties;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelayString = "#{@notificationScheduleDelay.value}")
    public void scheduledRun() {
        if (properties.isWorkerEnabled()) {
            runOnce();
        }
    }

    public int runOnce() {
        var claimed = transactions.claim(workerId);
        if (!claimed.isEmpty() && meterRegistry != null) {
            Counter.builder("repairauto.notification.worker.claimed")
                    .tag("channel", "telegram")
                    .register(meterRegistry)
                    .increment(claimed.size());
        }
        for (var notification : claimed) {
            OffsetDateTime startedAt = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
            var result = deliveryService.deliver(notification);
            transactions.finalizeDelivery(notification.notificationId(), workerId, startedAt, result);
            if (meterRegistry != null) {
                Counter.builder("repairauto.notification.delivery")
                        .tag("channel", "telegram")
                        .tag("outcome", result.outcome().name().toLowerCase(Locale.ROOT))
                        .register(meterRegistry)
                        .increment();
            }
            LOGGER.debug("Processed notification id={} outcome={}", notification.notificationId(), result.outcome());
        }
        return claimed.size();
    }
}
