package com.example.darks.repair_auto.identity.mobile.otp;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SmsStartupValidator implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmsStartupValidator.class);

    private final Environment environment;
    private final SmsProperties smsProperties;
    private final EskizProperties eskizProperties;
    private final PhoneOtpProperties phoneOtpProperties;

    public SmsStartupValidator(
            Environment environment,
            SmsProperties smsProperties,
            EskizProperties eskizProperties,
            PhoneOtpProperties phoneOtpProperties) {
        this.environment = environment;
        this.smsProperties = smsProperties;
        this.eskizProperties = eskizProperties;
        this.phoneOtpProperties = phoneOtpProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!isProduction) {
            return;
        }

        if (phoneOtpProperties.isExposeCodeInResponse()) {
            throw new IllegalStateException(
                    "CRITICAL SECURITY CONFIGURATION ERROR: app.phone-otp.expose-code-in-response must be false in production.");
        }

        if (smsProperties.isEnabled()) {
            if (smsProperties.getProvider() == SmsProviderType.LOGGING) {
                throw new IllegalStateException(
                        "CRITICAL SECURITY CONFIGURATION ERROR: LoggingSmsSender is strictly forbidden in production when SMS is enabled.");
            }
            if (smsProperties.getProvider() == SmsProviderType.ESKIZ) {
                if (eskizProperties.getEmail() == null || eskizProperties.getEmail().isBlank()
                        || eskizProperties.getPassword() == null || eskizProperties.getPassword().isBlank()) {
                    throw new IllegalStateException(
                            "CRITICAL CONFIGURATION ERROR: Eskiz credentials (email and password) are required in production when SMS is enabled.");
                }
            }
            LOGGER.info("Production SMS startup validation passed successfully with provider={}", smsProperties.getProvider());
        } else {
            LOGGER.info("SMS is disabled in production configuration.");
        }
    }
}
