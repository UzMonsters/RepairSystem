package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.infrastructure.worker.NotificationProperties;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationRetryPolicy {

    private static final double JITTER_RATIO = 0.15; // +/- 15%

    private final NotificationProperties properties;
    private final DoubleSupplier randomSupplier;

    @Autowired
    public NotificationRetryPolicy(NotificationProperties properties) {
        this(properties, () -> ThreadLocalRandom.current().nextDouble());
    }

    public NotificationRetryPolicy(NotificationProperties properties, DoubleSupplier randomSupplier) {
        this.properties = properties;
        this.randomSupplier = randomSupplier != null ? randomSupplier : () -> ThreadLocalRandom.current().nextDouble();
    }

    public Duration nextBackoff(int nextAttemptNumber) {
        int exponent = Math.max(0, nextAttemptNumber - 1);
        long initialMillis = properties.getInitialBackoff().toMillis();
        long maxMillis = properties.getMaxBackoff().toMillis();
        long multiplier = exponent >= 30 ? Long.MAX_VALUE : 1L << exponent;
        long baseMillis;
        try {
            baseMillis = Math.multiplyExact(initialMillis, multiplier);
        } catch (ArithmeticException exception) {
            baseMillis = maxMillis;
        }
        long cappedMillis = Math.min(maxMillis, baseMillis);

        // Apply bounded jitter: [1.0 - JITTER_RATIO, 1.0 + JITTER_RATIO]
        double randomVal = randomSupplier.getAsDouble(); // in [0.0, 1.0)
        double jitterMultiplier = (1.0 - JITTER_RATIO) + (2.0 * JITTER_RATIO * randomVal);
        long jitteredMillis = Math.round(cappedMillis * jitterMultiplier);

        return Duration.ofMillis(Math.max(1000L, Math.min(maxMillis, jitteredMillis)));
    }
}
