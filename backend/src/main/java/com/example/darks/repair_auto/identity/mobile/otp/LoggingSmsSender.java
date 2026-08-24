package com.example.darks.repair_auto.identity.mobile.otp;

import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingSmsSender implements SmsSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSmsSender.class);

    private final SmsProperties smsProperties;

    public LoggingSmsSender(SmsProperties smsProperties) {
        this.smsProperties = smsProperties;
    }

    @Override
    public boolean isEnabled() {
        return smsProperties.isEnabled() && smsProperties.getProvider() == SmsProviderType.LOGGING;
    }

    @Override
    public void sendOtp(String phoneNumber, String code, LanguageCode language) {
        if (!isEnabled()) {
            return;
        }
        String maskedPhone = maskPhone(phoneNumber);
        LOGGER.info("LoggingSmsSender: Dispatching SMS OTP to phone recipient={}", maskedPhone);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        String normalized = phone.trim();
        int len = normalized.length();
        return normalized.substring(0, Math.min(6, len)) + " *** ** " + normalized.substring(Math.max(0, len - 2));
    }
}
