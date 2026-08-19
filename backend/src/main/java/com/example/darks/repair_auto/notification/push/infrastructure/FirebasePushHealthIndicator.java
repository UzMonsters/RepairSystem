package com.example.darks.repair_auto.notification.push.infrastructure;

import com.example.darks.repair_auto.notification.push.config.FirebasePushProperties;
import com.example.darks.repair_auto.notification.push.config.FirebasePushStartupValidator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("firebasePushHealthIndicator")
public class FirebasePushHealthIndicator implements HealthIndicator {

    private final FirebasePushProperties properties;
    private final FirebasePushStartupValidator startupValidator;

    public FirebasePushHealthIndicator(
            FirebasePushProperties properties,
            FirebasePushStartupValidator startupValidator) {
        this.properties = properties;
        this.startupValidator = startupValidator;
    }

    @Override
    public Health health() {
        if (!properties.enabled()) {
            return Health.up()
                    .withDetail("status", "DISABLED")
                    .withDetail("enabled", false)
                    .build();
        }

        if (startupValidator.isValid()) {
            return Health.up()
                    .withDetail("status", "UP")
                    .withDetail("enabled", true)
                    .withDetail("projectId", properties.projectId())
                    .build();
        }

        return Health.down()
                .withDetail("status", "MISCONFIGURED")
                .withDetail("enabled", true)
                .withDetail("error", startupValidator.getValidationError() != null
                        ? startupValidator.getValidationError()
                        : "Firebase Push initialization or configuration failed")
                .build();
    }
}
