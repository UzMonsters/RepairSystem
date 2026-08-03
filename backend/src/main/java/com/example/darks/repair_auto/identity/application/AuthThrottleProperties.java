package com.example.darks.repair_auto.identity.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth-throttle")
public record AuthThrottleProperties(
        boolean enabled,
        int maxFailures,
        Duration window,
        Duration blockDuration,
        Duration retention
) {

    public AuthThrottleProperties {
        maxFailures = maxFailures <= 0 ? 5 : maxFailures;
        window = window == null ? Duration.ofMinutes(10) : window;
        blockDuration = blockDuration == null ? Duration.ofMinutes(15) : blockDuration;
        retention = retention == null ? Duration.ofDays(1) : retention;
    }
}
