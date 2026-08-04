package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.infrastructure.worker.NotificationProperties;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class NotificationRetryPolicy {

    private final NotificationProperties properties;

    public NotificationRetryPolicy(NotificationProperties properties) {
        this.properties = properties;
    }

    public Duration nextBackoff(int nextAttemptNumber) {
        int exponent = Math.max(0, nextAttemptNumber - 1);
        long initialMillis = properties.getInitialBackoff().toMillis();
        long maxMillis = properties.getMaxBackoff().toMillis();
        long multiplier = exponent >= 30 ? Long.MAX_VALUE : 1L << exponent;
        long backoffMillis;
        try {
            backoffMillis = Math.multiplyExact(initialMillis, multiplier);
        } catch (ArithmeticException exception) {
            backoffMillis = maxMillis;
        }
        return Duration.ofMillis(Math.min(maxMillis, backoffMillis));
    }
}
