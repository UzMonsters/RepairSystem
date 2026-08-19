package com.example.darks.repair_auto.notification.push.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.notification.push.config.FirebasePushProperties;
import com.example.darks.repair_auto.notification.push.config.FirebasePushStartupValidator;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

class FirebasePushHealthIndicatorTest {

    @Test
    void givenFirebaseDisabled_whenHealth_thenReturnsUpWithDisabledStatus() {
        FirebasePushProperties properties = new FirebasePushProperties(
                false, null, null, Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofDays(90), false, Duration.ofDays(1));
        FirebasePushStartupValidator validator = mock(FirebasePushStartupValidator.class);

        FirebasePushHealthIndicator indicator = new FirebasePushHealthIndicator(properties, validator);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("status", "DISABLED");
        assertThat(health.getDetails()).containsEntry("enabled", false);
    }

    @Test
    void givenFirebaseEnabledAndValid_whenHealth_thenReturnsUpWithProjectId() {
        FirebasePushProperties properties = new FirebasePushProperties(
                true, "repairauto-prod", null, Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofDays(90), false, Duration.ofDays(1));
        FirebasePushStartupValidator validator = mock(FirebasePushStartupValidator.class);
        when(validator.isValid()).thenReturn(true);

        FirebasePushHealthIndicator indicator = new FirebasePushHealthIndicator(properties, validator);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("status", "UP");
        assertThat(health.getDetails()).containsEntry("enabled", true);
        assertThat(health.getDetails()).containsEntry("projectId", "repairauto-prod");
    }

    @Test
    void givenFirebaseEnabledAndInvalid_whenHealth_thenReturnsDownWithMisconfiguredStatus() {
        FirebasePushProperties properties = new FirebasePushProperties(
                true, "repairauto-prod", null, Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofDays(90), false, Duration.ofDays(1));
        FirebasePushStartupValidator validator = mock(FirebasePushStartupValidator.class);
        when(validator.isValid()).thenReturn(false);
        when(validator.getValidationError()).thenReturn("Credentials missing");

        FirebasePushHealthIndicator indicator = new FirebasePushHealthIndicator(properties, validator);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("status", "MISCONFIGURED");
        assertThat(health.getDetails()).containsEntry("error", "Credentials missing");
    }
}
