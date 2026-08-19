package com.example.darks.repair_auto.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.notification.infrastructure.worker.NotificationProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class NotificationRetryPolicyJitterTest {

    @Test
    void givenNeutralRandomSupplier_whenNextBackoff_thenProducesExactExponentialBackoff() {
        NotificationProperties properties = new NotificationProperties();
        properties.setInitialBackoff(Duration.ofSeconds(10));
        properties.setMaxBackoff(Duration.ofMinutes(30));

        // 0.5 maps to exactly 1.0 jitter multiplier (0% jitter)
        NotificationRetryPolicy policy = new NotificationRetryPolicy(properties, () -> 0.5);

        assertThat(policy.nextBackoff(1)).isEqualTo(Duration.ofSeconds(10));
        assertThat(policy.nextBackoff(2)).isEqualTo(Duration.ofSeconds(20));
        assertThat(policy.nextBackoff(3)).isEqualTo(Duration.ofSeconds(40));
        assertThat(policy.nextBackoff(4)).isEqualTo(Duration.ofSeconds(80));
    }

    @Test
    void givenMinRandomSupplier_whenNextBackoff_thenAppliesMinus15PercentJitter() {
        NotificationProperties properties = new NotificationProperties();
        properties.setInitialBackoff(Duration.ofSeconds(10));
        properties.setMaxBackoff(Duration.ofMinutes(30));

        // 0.0 maps to 0.85 jitter multiplier (-15%)
        NotificationRetryPolicy policy = new NotificationRetryPolicy(properties, () -> 0.0);

        assertThat(policy.nextBackoff(1)).isEqualTo(Duration.ofMillis(8500));
        assertThat(policy.nextBackoff(2)).isEqualTo(Duration.ofMillis(17000));
    }

    @Test
    void givenMaxRandomSupplier_whenNextBackoff_thenAppliesPlus15PercentJitterCappedAtMax() {
        NotificationProperties properties = new NotificationProperties();
        properties.setInitialBackoff(Duration.ofSeconds(10));
        properties.setMaxBackoff(Duration.ofSeconds(30));

        // 1.0 maps to 1.15 jitter multiplier (+15%)
        NotificationRetryPolicy policy = new NotificationRetryPolicy(properties, () -> 1.0);

        assertThat(policy.nextBackoff(1)).isEqualTo(Duration.ofMillis(11500));
        assertThat(policy.nextBackoff(2)).isEqualTo(Duration.ofMillis(23000));
        // Capped at maxBackoff (30s)
        assertThat(policy.nextBackoff(3)).isEqualTo(Duration.ofSeconds(30));
    }
}
