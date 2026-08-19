package com.example.darks.repair_auto.notification.push.application;

import com.example.darks.repair_auto.notification.push.config.FirebasePushProperties;
import com.example.darks.repair_auto.notification.push.infrastructure.PushEndpointRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushEndpointStaleCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushEndpointStaleCleanupService.class);

    private final PushEndpointRepository repository;
    private final FirebasePushProperties properties;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    @Autowired
    public PushEndpointStaleCleanupService(
            PushEndpointRepository repository,
            FirebasePushProperties properties,
            MeterRegistry meterRegistry) {
        this(repository, properties, meterRegistry, Clock.systemUTC());
    }

    public PushEndpointStaleCleanupService(
            PushEndpointRepository repository,
            FirebasePushProperties properties,
            MeterRegistry meterRegistry,
            Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.firebase.stale-cleanup-interval:P1D}")
    public void scheduledRun() {
        if (properties.staleCleanupEnabled()) {
            disableStaleEndpoints();
        }
    }

    @Transactional
    public int disableStaleEndpoints() {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime threshold = now.minus(properties.endpointStaleAfter());
        int disabledCount = repository.disableStaleEndpoints(threshold, now);
        if (disabledCount > 0) {
            LOGGER.info("Disabled {} stale push endpoints inactive since before {}", disabledCount, threshold);
            Counter.builder("repairauto.push.endpoint.disabled")
                    .tag("reason", "stale")
                    .register(meterRegistry)
                    .increment(disabledCount);
        }
        return disabledCount;
    }
}
