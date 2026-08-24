package com.example.darks.repair_auto.identity.mobile.email;

import com.example.darks.repair_auto.shared.i18n.LanguageCode;

public interface EmailSender {

    void sendVerificationCode(String toEmail, String code, LanguageCode language);

    boolean isEnabled();
}
