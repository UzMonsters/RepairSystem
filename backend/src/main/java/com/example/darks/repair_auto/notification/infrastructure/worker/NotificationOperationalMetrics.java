package com.example.darks.repair_auto.notification.infrastructure.worker;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationOperationalMetrics {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public NotificationOperationalMetrics(JdbcTemplate jdbcTemplate, MeterRegistry registry, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        gauge(registry, "repairauto.notifications.pending", "PENDING");
        gauge(registry, "repairauto.notifications.processing", "PROCESSING");
        gauge(registry, "repairauto.notifications.retry_scheduled", "RETRY_SCHEDULED");
        gauge(registry, "repairauto.notifications.dead", "DEAD");
        Gauge.builder("repairauto.notifications.oldest_pending_age_seconds", this, metrics -> metrics.oldestPendingAge())
                .description("Age in seconds of the oldest pending or retry-scheduled notification.")
                .register(registry);
    }

    private void gauge(MeterRegistry registry, String name, String status) {
        Gauge.builder(name, this, metrics -> metrics.countStatus(status))
                .description("Notification outbox count for status " + status + ".")
                .register(registry);
    }

    private double countStatus(String status) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from notification_outbox where status = ?",
                Long.class,
                status);
        return count == null ? 0 : count;
    }

    private double oldestPendingAge() {
        OffsetDateTime oldest = jdbcTemplate.queryForObject("""
                select min(created_at)
                from notification_outbox
                where status in ('PENDING', 'RETRY_SCHEDULED')
                """, OffsetDateTime.class);
        if (oldest == null) {
            return 0;
        }
        return Math.max(0, java.time.Duration.between(oldest, now()).toSeconds());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
