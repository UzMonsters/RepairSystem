package com.example.darks.repair_auto.identity.mobile.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpEmailSenderTest {

    private ObjectProvider<JavaMailSender> mailSenderProvider;
    private JavaMailSender mailSender;
    private EmailVerificationProperties properties;
    private SmtpEmailSender emailSender;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mailSenderProvider = mock(ObjectProvider.class);
        mailSender = mock(JavaMailSender.class);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        properties = new EmailVerificationProperties();
        properties.setEnabled(true);
        properties.setFromAddress("no-reply@repairauto.uz");
        properties.setFromName("RepairAuto");

        emailSender = new SmtpEmailSender(mailSenderProvider, properties);
    }

    @Test
    void givenEnabledWhenSendVerificationCodeInEnglishThenSendsCorrectEmail() {
        emailSender.sendVerificationCode("user@example.com", "123456", LanguageCode.EN);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getFrom()).isEqualTo("RepairAuto <no-reply@repairauto.uz>");
        assertThat(message.getSubject()).isEqualTo("RepairAuto email verification");
        assertThat(message.getText()).contains("123456").contains("expires in 10 minutes");
    }

    @Test
    void givenEnabledWhenSendVerificationCodeInRussianThenSendsCorrectEmail() {
        emailSender.sendVerificationCode("user@example.com", "654321", LanguageCode.RU);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getSubject()).isEqualTo("Подтверждение электронной почты RepairAuto");
        assertThat(message.getText()).contains("654321").contains("действителен в течение 10 минут");
    }

    @Test
    void givenEnabledWhenSendVerificationCodeInUzbekThenSendsCorrectEmail() {
        emailSender.sendVerificationCode("user@example.com", "789012", LanguageCode.UZ);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getSubject()).isEqualTo("RepairAuto elektron pochtani tasdiqlash");
        assertThat(message.getText()).contains("789012").contains("10 daqiqa davomida amal qiladi");
    }

    @Test
    void givenDisabledWhenSendVerificationCodeThenDoesNothing() {
        properties.setEnabled(false);

        emailSender.sendVerificationCode("user@example.com", "123456", LanguageCode.EN);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void givenMailSenderNotAvailableWhenSendVerificationCodeThenThrowsException() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> emailSender.sendVerificationCode("user@example.com", "123456", LanguageCode.EN))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_DELIVERY_FAILED);
    }
}
