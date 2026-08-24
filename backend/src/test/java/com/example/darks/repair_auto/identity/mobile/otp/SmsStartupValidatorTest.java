package com.example.darks.repair_auto.identity.mobile.otp;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;

class SmsStartupValidatorTest {

    @Test
    void nonProductionEnvironment_passesValidationEvenIfLoggingProviderUsed() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"dev"});

        SmsProperties smsProps = SmsProperties.of(true, SmsProviderType.LOGGING);
        EskizProperties eskizProps = EskizProperties.of("", "", "", "", Duration.ZERO, Duration.ZERO);
        PhoneOtpProperties otpProps = PhoneOtpProperties.of(true, Duration.ofMinutes(5), Duration.ofSeconds(60), 5, true);

        SmsStartupValidator validator = new SmsStartupValidator(env, smsProps, eskizProps, otpProps);

        assertThatCode(() -> validator.run(mock(ApplicationArguments.class)))
                .doesNotThrowAnyException();
    }

    @Test
    void productionEnvironment_whenExposeCodeInResponseIsTrue_throwsIllegalStateException() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        SmsProperties smsProps = SmsProperties.of(true, SmsProviderType.ESKIZ);
        EskizProperties eskizProps = EskizProperties.of("https://notify.eskiz.uz", "admin@test.com", "pass", "4546", Duration.ZERO, Duration.ZERO);
        PhoneOtpProperties otpProps = PhoneOtpProperties.of(true, Duration.ofMinutes(5), Duration.ofSeconds(60), 5, true);

        SmsStartupValidator validator = new SmsStartupValidator(env, smsProps, eskizProps, otpProps);

        assertThatThrownBy(() -> validator.run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expose-code-in-response must be false in production");
    }

    @Test
    void productionEnvironment_whenLoggingProviderUsedAndSmsEnabled_throwsIllegalStateException() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        SmsProperties smsProps = SmsProperties.of(true, SmsProviderType.LOGGING);
        EskizProperties eskizProps = EskizProperties.of("https://notify.eskiz.uz", "admin@test.com", "pass", "4546", Duration.ZERO, Duration.ZERO);
        PhoneOtpProperties otpProps = PhoneOtpProperties.of(true, Duration.ofMinutes(5), Duration.ofSeconds(60), 5, false);

        SmsStartupValidator validator = new SmsStartupValidator(env, smsProps, eskizProps, otpProps);

        assertThatThrownBy(() -> validator.run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LoggingSmsSender is strictly forbidden in production");
    }

    @Test
    void productionEnvironment_whenEskizProviderHasMissingCredentials_throwsIllegalStateException() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        SmsProperties smsProps = SmsProperties.of(true, SmsProviderType.ESKIZ);
        EskizProperties eskizProps = EskizProperties.of("https://notify.eskiz.uz", "", "", "4546", Duration.ZERO, Duration.ZERO);
        PhoneOtpProperties otpProps = PhoneOtpProperties.of(true, Duration.ofMinutes(5), Duration.ofSeconds(60), 5, false);

        SmsStartupValidator validator = new SmsStartupValidator(env, smsProps, eskizProps, otpProps);

        assertThatThrownBy(() -> validator.run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Eskiz credentials (email and password) are required");
    }

    @Test
    void productionEnvironment_whenEskizConfiguredProperly_passesValidation() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        SmsProperties smsProps = SmsProperties.of(true, SmsProviderType.ESKIZ);
        EskizProperties eskizProps = EskizProperties.of("https://notify.eskiz.uz", "admin@repairauto.uz", "secure-eskiz-pass", "4546", Duration.ZERO, Duration.ZERO);
        PhoneOtpProperties otpProps = PhoneOtpProperties.of(true, Duration.ofMinutes(5), Duration.ofSeconds(60), 5, false);

        SmsStartupValidator validator = new SmsStartupValidator(env, smsProps, eskizProps, otpProps);

        assertThatCode(() -> validator.run(mock(ApplicationArguments.class)))
                .doesNotThrowAnyException();
    }
}
