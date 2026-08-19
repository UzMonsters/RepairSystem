package com.example.darks.repair_auto.notification.push.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.notification.push.config.FirebasePushProperties;
import com.example.darks.repair_auto.notification.push.infrastructure.PushEndpointRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PushEndpointStaleCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-18T10:00:00Z");

    private PushEndpointRepository repository;
    private SimpleMeterRegistry meterRegistry;
    private Clock clock;

    @BeforeEach
    void setUp() {
        repository = mock(PushEndpointRepository.class);
        meterRegistry = new SimpleMeterRegistry();
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Test
    void givenStaleCleanupDisabled_whenScheduledRun_thenDoesNotExecute() {
        FirebasePushProperties properties = new FirebasePushProperties(
                true, "repairauto-dev", null, Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofDays(90), false, Duration.ofDays(1));
        PushEndpointStaleCleanupService service = new PushEndpointStaleCleanupService(
                repository, properties, meterRegistry, clock);

        service.scheduledRun();

        verify(repository, never()).disableStaleEndpoints(any(), any());
    }

    @Test
    void givenStaleEndpointsPresent_whenDisableStaleEndpoints_thenUpdatesEndpointsAndRecordsMetric() {
        FirebasePushProperties properties = new FirebasePushProperties(
                true, "repairauto-dev", null, Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofDays(90), true, Duration.ofDays(1));
        PushEndpointStaleCleanupService service = new PushEndpointStaleCleanupService(
                repository, properties, meterRegistry, clock);

        OffsetDateTime expectedThreshold = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).minusDays(90);
        OffsetDateTime expectedNow = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

        when(repository.disableStaleEndpoints(eq(expectedThreshold), eq(expectedNow))).thenReturn(4);

        int count = service.disableStaleEndpoints();

        assertThat(count).isEqualTo(4);
        verify(repository).disableStaleEndpoints(expectedThreshold, expectedNow);
        assertThat(meterRegistry.get("repairauto.push.endpoint.disabled")
                .tag("reason", "stale")
                .counter()
                .count()).isEqualTo(4.0);
    }
}
