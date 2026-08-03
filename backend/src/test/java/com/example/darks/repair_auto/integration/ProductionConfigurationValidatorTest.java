package com.example.darks.repair_auto.integration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.darks.repair_auto.dashboard.application.DashboardProperties;
import com.example.darks.repair_auto.identity.application.AuthThrottleProperties;
import com.example.darks.repair_auto.notification.infrastructure.worker.NotificationProperties;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageProperties;
import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.shared.config.ProductionConfigurationValidator;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramProperties;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.unit.DataSize;

class ProductionConfigurationValidatorTest {

    @Test
    void givenProdProfileWithMissingRequiredSettingsThenStartupFailsWithSanitizedMessage() {
        ProductionConfigurationValidator validator = validator(environment(), appProperties());

        assertThatThrownBy(validator::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PRODUCTION_CONFIGURATION_INVALID")
                .hasMessageContaining("SPRING_DATASOURCE_URL")
                .hasMessageNotContaining("secret-value");
    }

    @Test
    void givenProdProfileWithWildcardCorsThenStartupFails() {
        MockEnvironment environment = validEnvironment()
                .withProperty("APP_CORS_ALLOWED_ORIGINS", "*");
        AppProperties appProperties = appProperties(new AppProperties.Cors(
                List.of("*"),
                List.of("GET", "POST"),
                List.of("*"),
                List.of("X-Trace-Id"),
                false));

        assertThatThrownBy(() -> validator(environment, appProperties).afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Wildcard CORS origins are not allowed");
    }

    @Test
    void givenProdProfileWithValidHardeningSettingsThenStartupValidationPasses() {
        assertThatCode(() -> validator(validEnvironment(), appProperties()).afterSingletonsInstantiated())
                .doesNotThrowAnyException();
    }

    private ProductionConfigurationValidator validator(MockEnvironment environment, AppProperties appProperties) {
        TelegramProperties telegramProperties = new TelegramProperties();
        telegramProperties.setEnabled(false);
        NotificationProperties notificationProperties = new NotificationProperties();
        notificationProperties.setBatchSize(50);
        return new ProductionConfigurationValidator(
                environment,
                appProperties,
                storageProperties(),
                telegramProperties,
                new DashboardProperties("Asia/Tashkent"),
                notificationProperties,
                new AuthThrottleProperties(true, 5, Duration.ofMinutes(10), Duration.ofMinutes(15), Duration.ofDays(1)));
    }

    private MockEnvironment validEnvironment() {
        return environment()
                .withProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://db:5432/repair_auto")
                .withProperty("SPRING_DATASOURCE_USERNAME", "repair_auto")
                .withProperty("SPRING_DATASOURCE_PASSWORD", "secret-value")
                .withProperty("APP_JWT_SECRET", "strong-production-secret-value-123456")
                .withProperty("APP_BOOTSTRAP_ADMIN_ENABLED", "false")
                .withProperty("APP_CORS_ALLOWED_ORIGINS", "https://admin.repairauto.example")
                .withProperty("APP_DASHBOARD_BUSINESS_TIME_ZONE", "Asia/Tashkent")
                .withProperty("spring.flyway.validate-on-migrate", "true")
                .withProperty("spring.flyway.clean-disabled", "true")
                .withProperty("spring.flyway.out-of-order", "false");
    }

    private MockEnvironment environment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment
                .withProperty("spring.flyway.validate-on-migrate", "true")
                .withProperty("spring.flyway.clean-disabled", "true")
                .withProperty("spring.flyway.out-of-order", "false");
    }

    private AppProperties appProperties() {
        return appProperties(new AppProperties.Cors(
                List.of("https://admin.repairauto.example"),
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
                List.of("*"),
                List.of("X-Trace-Id"),
                false));
    }

    private AppProperties appProperties(AppProperties.Cors cors) {
        return new AppProperties(
                cors,
                new AppProperties.Trace("X-Trace-Id"),
                new AppProperties.Jwt(
                        "strong-production-secret-value-123456",
                        "repair-auto",
                        Duration.ofMinutes(15)),
                Duration.ofDays(30),
                new AppProperties.BootstrapAdmin(false, "", "", "System Administrator"));
    }

    private StorageProperties storageProperties() {
        return new StorageProperties(
                true,
                URI.create("https://storage.repairauto.example"),
                "us-east-1",
                "repairauto-prod",
                "access-key",
                "secret-key",
                true,
                false,
                Duration.ofMinutes(5),
                DataSize.ofMegabytes(10),
                20,
                10);
    }
}
