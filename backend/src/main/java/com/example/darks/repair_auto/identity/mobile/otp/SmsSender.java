package com.example.darks.repair_auto.identity.mobile.otp;

import com.example.darks.repair_auto.shared.i18n.LanguageCode;

public interface SmsSender {

    void sendOtp(String phoneNumber, String code, LanguageCode language);

    boolean isEnabled();
}
