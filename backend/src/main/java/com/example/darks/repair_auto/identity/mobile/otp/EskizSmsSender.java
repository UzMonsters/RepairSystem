package com.example.darks.repair_auto.identity.mobile.otp;

import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import org.springframework.stereotype.Component;

@Component
public class EskizSmsSender implements SmsSender {

    private final SmsProperties smsProperties;
    private final EskizClient eskizClient;

    public EskizSmsSender(SmsProperties smsProperties, EskizClient eskizClient) {
        this.smsProperties = smsProperties;
        this.eskizClient = eskizClient;
    }

    @Override
    public boolean isEnabled() {
        return smsProperties.isEnabled() && smsProperties.getProvider() == SmsProviderType.ESKIZ;
    }

    @Override
    public void sendOtp(String phoneNumber, String code, LanguageCode language) {
        if (!isEnabled()) {
            return;
        }
        String message = buildMessage(code, language);
        eskizClient.sendSms(phoneNumber, message);
    }

    String buildMessage(String code, LanguageCode language) {
        LanguageCode lang = language == null ? LanguageCode.UZ : language;
        return switch (lang) {
            case RU -> "Код подтверждения RepairAuto: " + code + ". Код действует 5 минут. Никому его не сообщайте.";
            case EN -> "Your RepairAuto verification code is: " + code + ". The code expires in 5 minutes. Do not share it with anyone.";
            case UZ -> "RepairAuto tasdiqlash kodi: " + code + ". Kod 5 daqiqa amal qiladi. Uni hech kimga bermang.";
        };
    }
}
