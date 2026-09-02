package com.example.darks.repair_auto.shared.config;

import com.example.darks.repair_auto.identity.application.AuthThrottleProperties;
import com.example.darks.repair_auto.identity.mobile.google.GoogleOidcProperties;
import com.example.darks.repair_auto.identity.mobile.telegram.TelegramLoginProperties;
import com.example.darks.repair_auto.notification.infrastructure.worker.NotificationProperties;
import com.example.darks.repair_auto.notification.push.config.FirebasePushProperties;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageProperties;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramProperties;
import java.net.URI;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Arrays;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionConfigurationValidator implements SmartInitializingSingleton {

    private static final int MIN_SECRET_LENGTH = 32;

    private final Environment environment;
    private final AppProperties appProperties;
    private final StorageProperties storageProperties;
    private final TelegramProperties telegramProperties;
    private final ZoneId businessZone;
    private final NotificationProperties notificationProperties;
    private final AuthThrottleProperties authThrottleProperties;
    private final FirebasePushProperties firebasePushProperties;
    private final TelegramLoginProperties telegramLoginProperties;
    private final GoogleOidcProperties googleOidcProperties;

    public ProductionConfigurationValidator(
            Environment environment,
            AppProperties appProperties,
            StorageProperties storageProperties,
            TelegramProperties telegramProperties,
            ZoneId businessZone,
            NotificationProperties notificationProperties,
            AuthThrottleProperties authThrottleProperties,
            FirebasePushProperties firebasePushProperties,
            TelegramLoginProperties telegramLoginProperties,
            GoogleOidcProperties googleOidcProperties) {
        this.environment = environment;
        this.appProperties = appProperties;
        this.storageProperties = storageProperties;
        this.telegramProperties = telegramProperties;
        this.businessZone = businessZone;
        this.notificationProperties = notificationProperties;
        this.authThrottleProperties = authThrottleProperties;
        this.firebasePushProperties = firebasePushProperties;
        this.telegramLoginProperties = telegramLoginProperties;
        this.googleOidcProperties = googleOidcProperties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!isProd()) {
            return;
        }
        require("spring.datasource.url");
        require("spring.datasource.username");
        require("spring.datasource.password");
        require("APP_JWT_SECRET");
        require("APP_BOOTSTRAP_ADMIN_ENABLED");
        require("APP_CORS_ALLOWED_ORIGINS");
        require("APP_BUSINESS_TIME_ZONE");
        validateJwt();
        validateCors();
        validateStorage();
        validateTelegram();
        validateGoogle();
        validateFirebasePush();
        validateDurations();
        validateFlyway();
    }

    private void validateJwt() {
        AppProperties.Jwt jwt = appProperties.jwt();
        String secret = firstNonBlank(
                environment.getProperty("app.jwt.secret"),
                environment.getProperty("APP_JWT_SECRET"),
                jwt == null ? null : jwt.secret());
        if (secret == null || secret.length() < MIN_SECRET_LENGTH || secret.contains("local-only")) {
            fail("APP_JWT_SECRET must be a strong production secret.");
        }
        Duration accessTokenTtl = environment.getProperty("app.jwt.access-token-ttl", Duration.class);
        if (accessTokenTtl == null && jwt != null) {
            accessTokenTtl = jwt.accessTokenTtl();
        }
        if (accessTokenTtl == null
                || accessTokenTtl.isNegative()
                || accessTokenTtl.isZero()
                || accessTokenTtl.compareTo(Duration.ofHours(1)) > 0) {
            fail("APP_JWT_ACCESS_TOKEN_TTL must be positive and no longer than PT1H in production.");
        }
    }

    private void validateCors() {
        AppProperties.Cors cors = appProperties.cors();
        if (cors.allowedOrigins() == null || cors.allowedOrigins().isEmpty()) {
            fail("APP_CORS_ALLOWED_ORIGINS is required in production.");
        }
        if (cors.allowedOrigins().contains("*")) {
            fail("Wildcard CORS origins are not allowed in production.");
        }
        for (String origin : cors.allowedOrigins()) {
            URI uri = URI.create(origin);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                fail("Production CORS origins must use HTTPS.");
            }
        }
        if (cors.allowCredentials()) {
            fail("CORS credentials must remain disabled for the API.");
        }
    }

    private void validateStorage() {
        if (!storageProperties.enabled()) {
            return;
        }
        if (storageProperties.endpoint() == null) {
            fail("APP_STORAGE_ENDPOINT is required in production.");
        }
        requireHttpsOrHttp(storageProperties.endpoint(), "APP_STORAGE_ENDPOINT");
        requirePresent(storageProperties.region(), "APP_STORAGE_REGION");
        requirePresent(storageProperties.bucket(), "APP_STORAGE_BUCKET");
        requirePresent(storageProperties.accessKey(), "APP_STORAGE_ACCESS_KEY");
        requirePresent(storageProperties.secretKey(), "APP_STORAGE_SECRET_KEY");
        if (storageProperties.maxFileSize().toBytes() <= 0) {
            fail("APP_STORAGE_MAX_FILE_SIZE must be positive.");
        }
    }

    private void validateTelegram() {
        if (!telegramProperties.isEnabled()) {
            return;
        }
        validateTelegramBot(telegramProperties.getCustomer(), "CUSTOMER");
        validateTelegramBot(telegramProperties.getTechnician(), "TECHNICIAN");
        requirePresent(telegramLoginProperties.getCustomer().getClientId(), "APP_TELEGRAM_CUSTOMER_LOGIN_CLIENT_ID");
        requirePresent(telegramLoginProperties.getTechnician().getClientId(), "APP_TELEGRAM_TECHNICIAN_LOGIN_CLIENT_ID");
        requireHttpsOrHttp(telegramProperties.getApiBaseUrl(), "APP_TELEGRAM_API_BASE_URL");
        requireHttpsOrHttp(telegramProperties.getFileBaseUrl(), "APP_TELEGRAM_FILE_BASE_URL");
        if (!telegramProperties.getApiBaseUrl().getScheme().equals("https")
                || !telegramProperties.getFileBaseUrl().getScheme().equals("https")) {
            fail("Telegram API URLs must use HTTPS in production.");
        }
    }

    private void validateGoogle() {
        if (googleOidcProperties == null || !googleOidcProperties.isEnabled()) {
            return;
        }
        if (googleOidcProperties.getCustomerAllowedAudiences().isEmpty()) {
            fail("APP_GOOGLE_OIDC_CUSTOMER_ALLOWED_AUDIENCES is required in production when Google login is enabled.");
        }
        if (googleOidcProperties.getTechnicianAllowedAudiences().isEmpty()) {
            fail("APP_GOOGLE_OIDC_TECHNICIAN_ALLOWED_AUDIENCES is required in production when Google login is enabled.");
        }
    }

    private void validateFirebasePush() {
        if (firebasePushProperties == null || !firebasePushProperties.enabled()) {
            return;
        }
        require("APP_FIREBASE_PROJECT_ID");
        String projectId = firebasePushProperties.projectId();
        requirePresent(projectId, "APP_FIREBASE_PROJECT_ID");
        if ("repairauto-dev".equalsIgnoreCase(projectId.trim()) || projectId.toLowerCase().contains("-dev")) {
            fail("APP_FIREBASE_PROJECT_ID must point to a production Firebase project when Firebase Push is enabled.");
        }
        String configuredPath = firstNonBlank(
                environment.getProperty("APP_FIREBASE_CREDENTIALS_PATH"),
                environment.getProperty("GOOGLE_APPLICATION_CREDENTIALS"));
        requirePresent(configuredPath, "APP_FIREBASE_CREDENTIALS_PATH or GOOGLE_APPLICATION_CREDENTIALS");
    }

    private void validateTelegramBot(TelegramProperties.Bot bot, String name) {
        requirePresent(bot.getBotToken(), "APP_TELEGRAM_" + name + "_BOT_TOKEN");
        requirePresent(bot.getWebhookSecret(), "APP_TELEGRAM_" + name + "_WEBHOOK_SECRET");
        requirePresent(bot.getBotUsername(), "APP_TELEGRAM_" + name + "_BOT_USERNAME");
        if (bot.getWebhookSecret().length() < MIN_SECRET_LENGTH) {
            fail("APP_TELEGRAM_" + name + "_WEBHOOK_SECRET must be a strong production secret.");
        }
    }

    private void validateDurations() {
        validatePositive(appProperties.refreshTokenTtl(), "APP_REFRESH_TOKEN_TTL");
        validatePositive(appProperties.rememberMeRefreshTokenTtl(), "APP_REMEMBER_ME_REFRESH_TOKEN_TTL");
        if (appProperties.rememberMeRefreshTokenTtl().compareTo(appProperties.refreshTokenTtl()) <= 0) {
            fail("APP_REMEMBER_ME_REFRESH_TOKEN_TTL must be greater than APP_REFRESH_TOKEN_TTL.");
        }
        validatePositive(authThrottleProperties.window(), "APP_AUTH_THROTTLE_WINDOW");
        validatePositive(authThrottleProperties.blockDuration(), "APP_AUTH_THROTTLE_BLOCK_DURATION");
        validatePositive(notificationProperties.getPollInterval(), "APP_NOTIFICATION_POLL_INTERVAL");
        validatePositive(notificationProperties.getProcessingLease(), "APP_NOTIFICATION_PROCESSING_LEASE");
        validatePositive(notificationProperties.getInitialBackoff(), "APP_NOTIFICATION_INITIAL_BACKOFF");
        validatePositive(notificationProperties.getMaxBackoff(), "APP_NOTIFICATION_MAX_BACKOFF");
        if (notificationProperties.getBatchSize() > 500) {
            fail("APP_NOTIFICATION_BATCH_SIZE must be 500 or less in production.");
        }
        try {
            ZoneId.of(businessZone.getId());
        } catch (DateTimeException exception) {
            fail("APP_BUSINESS_TIME_ZONE must be a valid timezone.");
        }
    }

    private void validateFlyway() {
        if (!environment.getProperty("spring.flyway.validate-on-migrate", Boolean.class, false)
                || !environment.getProperty("spring.flyway.clean-disabled", Boolean.class, true)
                || environment.getProperty("spring.flyway.out-of-order", Boolean.class, true)) {
            fail("Production Flyway must validate migrations, disable clean, and reject out-of-order migrations.");
        }
    }

    private void validatePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            fail(name + " must be positive.");
        }
    }

    private void require(String environmentVariable) {
        requirePresent(environment.getProperty(environmentVariable), environmentVariable);
    }

    private void requirePresent(String value, String name) {
        if (value == null || value.isBlank()) {
            fail(name + " is required in production.");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void requireHttpsOrHttp(URI uri, String name) {
        if (uri == null || (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))) {
            fail(name + " must be an HTTP(S) URI.");
        }
    }

    private boolean isProd() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private void fail(String message) {
        throw new IllegalStateException("PRODUCTION_CONFIGURATION_INVALID: " + message);
    }
}
