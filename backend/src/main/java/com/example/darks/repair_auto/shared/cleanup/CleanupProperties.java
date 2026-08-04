package com.example.darks.repair_auto.shared.cleanup;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cleanup")
public record CleanupProperties(
        boolean enabled,
        int batchSize,
        Duration interval,
        Duration staleUploadThreshold,
        Duration deletedObjectRetention,
        Duration refreshSessionRetention,
        Duration telegramUpdateRetention,
        Duration notificationAttemptRetention
) {

    public CleanupProperties {
        batchSize = batchSize <= 0 ? 100 : Math.min(batchSize, 1000);
        interval = interval == null ? Duration.ofMinutes(15) : interval;
        staleUploadThreshold = staleUploadThreshold == null ? Duration.ofHours(1) : staleUploadThreshold;
        deletedObjectRetention = deletedObjectRetention == null ? Duration.ofDays(7) : deletedObjectRetention;
        refreshSessionRetention = refreshSessionRetention == null ? Duration.ofDays(30) : refreshSessionRetention;
        telegramUpdateRetention = telegramUpdateRetention == null ? Duration.ofDays(30) : telegramUpdateRetention;
        notificationAttemptRetention = notificationAttemptRetention == null
                ? Duration.ofDays(30) : notificationAttemptRetention;
    }
}
