package com.example.darks.repair_auto.notification.push.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "app.firebase")
public record FirebasePushProperties(
        boolean enabled,
        String projectId,
        String credentialsPath,
        Duration connectTimeout,
        Duration readTimeout,
        Duration endpointStaleAfter,
        boolean staleCleanupEnabled,
        Duration staleCleanupInterval
) {
    public FirebasePushProperties(
            boolean enabled,
            String projectId,
            String credentialsPath,
            Duration connectTimeout,
            Duration readTimeout) {
        this(enabled, projectId, credentialsPath, connectTimeout, readTimeout, Duration.ofDays(90), false, Duration.ofDays(1));
    }

    @ConstructorBinding
    public FirebasePushProperties {
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(10);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(10);
        }
        if (endpointStaleAfter == null) {
            endpointStaleAfter = Duration.ofDays(90);
        }
        if (staleCleanupInterval == null) {
            staleCleanupInterval = Duration.ofDays(1);
        }
    }
}
