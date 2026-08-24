package com.example.darks.repair_auto.identity.mobile.otp;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableConfigurationProperties({SmsProperties.class, EskizProperties.class, PhoneOtpProperties.class})
public class SmsConfiguration {

    @Bean
    @Primary
    public SmsSender smsSender(
            SmsProperties smsProperties,
            EskizSmsSender eskizSmsSender,
            LoggingSmsSender loggingSmsSender) {
        if (smsProperties.getProvider() == SmsProviderType.ESKIZ) {
            return eskizSmsSender;
        }
        return loggingSmsSender;
    }
}
