package com.example.darks.repair_auto.integration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.darks.repair_auto.identity.application.AuthThrottleProperties;
import com.example.darks.repair_auto.identity.mobile.google.GoogleOidcProperties;
import com.example.darks.repair_auto.identity.mobile.telegram.TelegramLoginProperties;
import com.example.darks.repair_auto.notification.infrastructure.worker.NotificationProperties;
import com.example.darks.repair_auto.notification.push.config.FirebasePushProperties;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageProperties;
import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.shared.config.ProductionConfigurationValidator;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramProperties;
import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
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
                .hasMessageContaining("spring.datasource.url")
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

    @Test
    void givenProdProfileWithFirebaseEnabledAndDevProjectThenStartupFails() {
        MockEnvironment environment = validEnvironment()
                .withProperty("APP_FIREBASE_PROJECT_ID", "repairauto-dev")
                .withProperty("APP_FIREBASE_CREDENTIALS_PATH", "/etc/secrets/firebase-adminsdk.json");

        assertThatThrownBy(() -> validator(
                environment,
                appProperties(),
                firebasePushProperties(true, "repairauto-dev", "/etc/secrets/firebase-adminsdk.json"),
                telegramLoginProperties()).afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_FIREBASE_PROJECT_ID must point to a production Firebase project");
    }

    @Test
    void givenProdProfileWithFirebaseEnabledAndMissingCredentialSourceThenStartupFails() {
        MockEnvironment environment = validEnvironment()
                .withProperty("APP_FIREBASE_PROJECT_ID", "repairauto-prod");

        assertThatThrownBy(() -> validator(
                environment,
                appProperties(),
                firebasePushProperties(true, "repairauto-prod", ""),
                telegramLoginProperties()).afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_FIREBASE_CREDENTIALS_PATH or GOOGLE_APPLICATION_CREDENTIALS");
    }

    @Test
    void givenProdProfileWithTelegramEnabledAndMissingLoginClientsThenStartupFails() {
        TelegramProperties telegramProperties = telegramProperties(true);

        assertThatThrownBy(() -> validator(
                validEnvironment(),
                appProperties(),
                storageProperties(),
                telegramProperties,
                firebasePushProperties(false, "repairauto-dev", ""),
                new TelegramLoginProperties()).afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_TELEGRAM_CUSTOMER_LOGIN_CLIENT_ID");
    }

    @Test
    void givenProdProfileWithGoogleEnabledAndMissingAudiencesThenStartupFails() {
        assertThatThrownBy(() -> validator(
                validEnvironment(),
                appProperties(),
                storageProperties(),
                telegramProperties(false),
                firebasePushProperties(false, "repairauto-dev", ""),
                telegramLoginProperties(),
                new GoogleOidcProperties()).afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_GOOGLE_OIDC_CUSTOMER_ALLOWED_AUDIENCES");
    }

    private ProductionConfigurationValidator validator(MockEnvironment environment, AppProperties appProperties) {
        return validator(environment, appProperties, firebasePushProperties(false, "repairauto-dev", ""), telegramLoginProperties());
    }

    private ProductionConfigurationValidator validator(
            MockEnvironment environment,
            AppProperties appProperties,
            FirebasePushProperties firebasePushProperties,
            TelegramLoginProperties telegramLoginProperties) {
        return validator(
                environment,
                appProperties,
                storageProperties(),
                telegramProperties(false),
                firebasePushProperties,
                telegramLoginProperties,
                googleOidcProperties());
    }

    private ProductionConfigurationValidator validator(
            MockEnvironment environment,
            AppProperties appProperties,
            StorageProperties storageProperties,
            TelegramProperties telegramProperties,
            FirebasePushProperties firebasePushProperties,
            TelegramLoginProperties telegramLoginProperties) {
        return validator(
                environment,
                appProperties,
                storageProperties,
                telegramProperties,
                firebasePushProperties,
                telegramLoginProperties,
                googleOidcProperties());
    }

    private ProductionConfigurationValidator validator(
            MockEnvironment environment,
            AppProperties appProperties,
            StorageProperties storageProperties,
            TelegramProperties telegramProperties,
            FirebasePushProperties firebasePushProperties,
            TelegramLoginProperties telegramLoginProperties,
            GoogleOidcProperties googleOidcProperties) {
        NotificationProperties notificationProperties = new NotificationProperties();
        notificationProperties.setBatchSize(50);
        return new ProductionConfigurationValidator(
                environment,
                appProperties,
                storageProperties,
                telegramProperties,
                ZoneId.of("Asia/Tashkent"),
                notificationProperties,
                new AuthThrottleProperties(true, 5, Duration.ofMinutes(10), Duration.ofMinutes(15), Duration.ofDays(1)),
                firebasePushProperties,
                telegramLoginProperties,
                googleOidcProperties);
    }

    private MockEnvironment validEnvironment() {
        return environment()
                .withProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://db:5432/repair_auto")
                .withProperty("SPRING_DATASOURCE_USERNAME", "repair_auto")
                .withProperty("SPRING_DATASOURCE_PASSWORD", "secret-value")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/repair_auto")
                .withProperty("spring.datasource.username", "repair_auto")
                .withProperty("spring.datasource.password", "secret-value")
                .withProperty("APP_JWT_SECRET", "strong-production-secret-value-123456")
                .withProperty("APP_BOOTSTRAP_ADMIN_ENABLED", "false")
                .withProperty("APP_CORS_ALLOWED_ORIGINS", "https://admin.repairauto.example")
                .withProperty("APP_BUSINESS_TIME_ZONE", "Asia/Tashkent")
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
                Duration.ofDays(1),
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

    private FirebasePushProperties firebasePushProperties(boolean enabled, String projectId, String credentialsPath) {
        return new FirebasePushProperties(
                enabled,
                projectId,
                credentialsPath,
                Duration.ofSeconds(10),
                Duration.ofSeconds(10));
    }

    private TelegramProperties telegramProperties(boolean enabled) {
        TelegramProperties telegramProperties = new TelegramProperties();
        telegramProperties.setEnabled(enabled);
        telegramProperties.getCustomer().setBotToken("123456:customer-production-token");
        telegramProperties.getCustomer().setWebhookSecret("customer-webhook-secret-value-123456");
        telegramProperties.getCustomer().setBotUsername("repairauto_bot");
        telegramProperties.getTechnician().setBotToken("123456:technician-production-token");
        telegramProperties.getTechnician().setWebhookSecret("technician-webhook-secret-value-123456");
        telegramProperties.getTechnician().setBotUsername("repairauto_staff_bot");
        return telegramProperties;
    }

    private TelegramLoginProperties telegramLoginProperties() {
        TelegramLoginProperties properties = new TelegramLoginProperties();
        properties.getCustomer().setClientId("customer-login-client");
        properties.getTechnician().setClientId("technician-login-client");
        return properties;
    }

    private GoogleOidcProperties googleOidcProperties() {
        GoogleOidcProperties properties = new GoogleOidcProperties();
        properties.setCustomerAllowedAudiences(List.of("customer-web-client-id.apps.googleusercontent.com"));
        properties.setTechnicianAllowedAudiences(List.of("technician-web-client-id.apps.googleusercontent.com"));
        return properties;
    }
}
