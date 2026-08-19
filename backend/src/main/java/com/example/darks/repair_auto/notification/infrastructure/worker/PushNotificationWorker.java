package com.example.darks.repair_auto.notification.infrastructure.worker;

import com.example.darks.repair_auto.notification.application.PushNotificationDispatchService;
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
public class PushNotificationWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushNotificationWorker.class);

    private final PushNotificationWorkerTransactions transactions;
    private final PushNotificationDispatchService dispatchService;
    private final NotificationProperties properties;
    private final Clock clock;
    private final MeterRegistry meterRegistry;
    private final String workerId = "push-worker-" + UUID.randomUUID();

    @Autowired
    public PushNotificationWorker(
            PushNotificationWorkerTransactions transactions,
            PushNotificationDispatchService dispatchService,
            NotificationProperties properties,
            MeterRegistry meterRegistry) {
        this(transactions, dispatchService, properties, Clock.systemUTC(), meterRegistry);
    }

    PushNotificationWorker(
            PushNotificationWorkerTransactions transactions,
            PushNotificationDispatchService dispatchService,
            NotificationProperties properties,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.transactions = transactions;
        this.dispatchService = dispatchService;
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
                    .tag("channel", "push")
                    .register(meterRegistry)
                    .increment(claimed.size());
        }
        for (var notification : claimed) {
            OffsetDateTime startedAt = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
            var result = dispatchService.dispatch(notification);
            transactions.finalizeDelivery(notification.getId(), workerId, startedAt, result);
            if (meterRegistry != null) {
                Counter.builder("repairauto.notification.delivery")
                        .tag("channel", "push")
                        .tag("outcome", result.outcome().name().toLowerCase(Locale.ROOT))
                        .register(meterRegistry)
                        .increment();
            }
            LOGGER.debug("Processed push notification id={} outcome={}", notification.getId(), result.outcome());
        }
        return claimed.size();
    }
}
