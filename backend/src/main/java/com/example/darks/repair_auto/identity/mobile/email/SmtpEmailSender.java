package com.example.darks.repair_auto.identity.mobile.email;

import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender implements EmailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailVerificationProperties properties;

    public SmtpEmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            EmailVerificationProperties properties) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public void sendVerificationCode(String toEmail, String code, LanguageCode language) {
        if (!isEnabled()) {
            LOGGER.debug("Email delivery is disabled. Skipping sending verification code to {}", toEmail);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.warn("JavaMailSender is not available but email delivery is enabled.");
            throw new BusinessException(ErrorCode.EMAIL_DELIVERY_FAILED);
        }

        try {
            LanguageCode lang = language == null ? LanguageCode.UZ : language;
            String subject = resolveSubject(lang);
            String body = resolveBody(code, lang);

            SimpleMailMessage message = new SimpleMailMessage();
            if (properties.getFromName() != null && !properties.getFromName().isBlank()) {
                message.setFrom("%s <%s>".formatted(properties.getFromName().trim(), properties.getFromAddress()));
            } else {
                message.setFrom(properties.getFromAddress());
            }
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            LOGGER.info("Email verification code sent successfully to recipient.");
        } catch (Exception exception) {
            LOGGER.error("Failed to send email verification message: {}", exception.getMessage());
            throw new BusinessException(ErrorCode.EMAIL_DELIVERY_FAILED);
        }
    }

    private String resolveSubject(LanguageCode language) {
        return switch (language) {
            case EN -> "RepairAuto email verification";
            case RU -> "Подтверждение электронной почты RepairAuto";
            case UZ -> "RepairAuto elektron pochtani tasdiqlash";
        };
    }

    private String resolveBody(String code, LanguageCode language) {
        return switch (language) {
            case EN -> """
                    Your RepairAuto verification code is:

                    %s

                    The code expires in 10 minutes.

                    Do not share this code with anyone.
                    """.formatted(code).stripIndent();
            case RU -> """
                    Ваш код подтверждения RepairAuto:

                    %s

                    Код действителен в течение 10 минут.

                    Никому не сообщайте этот код.
                    """.formatted(code).stripIndent();
            case UZ -> """
                    Sizning RepairAuto tasdiqlash kodingiz:

                    %s

                    Kod 10 daqiqa davomida amal qiladi.

                    Ushbu kodni hech kimga bermang.
                    """.formatted(code).stripIndent();
        };
    }
}
