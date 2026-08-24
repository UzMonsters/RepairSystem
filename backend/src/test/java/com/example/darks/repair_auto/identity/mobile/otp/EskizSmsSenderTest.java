package com.example.darks.repair_auto.identity.mobile.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import org.junit.jupiter.api.Test;

class EskizSmsSenderTest {

    @Test
    void isEnabled_reflectsSmsProperties() {
        EskizClient client = mock(EskizClient.class);

        SmsProperties enabledProps = SmsProperties.of(true, SmsProviderType.ESKIZ);
        EskizSmsSender enabledSender = new EskizSmsSender(enabledProps, client);
        assertThat(enabledSender.isEnabled()).isTrue();

        SmsProperties disabledProps = SmsProperties.of(false, SmsProviderType.LOGGING);
        EskizSmsSender disabledSender = new EskizSmsSender(disabledProps, client);
        assertThat(disabledSender.isEnabled()).isFalse();
    }

    @Test
    void sendOtp_whenDisabled_doesNotInvokeClient() {
        EskizClient client = mock(EskizClient.class);
        SmsProperties props = SmsProperties.of(false, SmsProviderType.ESKIZ);
        EskizSmsSender sender = new EskizSmsSender(props, client);

        sender.sendOtp("+998901234567", "123456", LanguageCode.UZ);

        verify(client, never()).sendSms(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void sendOtp_uzbekLanguage_sendsUzbekTemplate() {
        EskizClient client = mock(EskizClient.class);
        SmsProperties props = SmsProperties.of(true, SmsProviderType.ESKIZ);
        EskizSmsSender sender = new EskizSmsSender(props, client);

        sender.sendOtp("+998901234567", "456789", LanguageCode.UZ);

        verify(client).sendSms(
                "+998901234567",
                "RepairAuto tasdiqlash kodi: 456789. Kod 5 daqiqa amal qiladi. Uni hech kimga bermang.");
    }

    @Test
    void sendOtp_russianLanguage_sendsRussianTemplate() {
        EskizClient client = mock(EskizClient.class);
        SmsProperties props = SmsProperties.of(true, SmsProviderType.ESKIZ);
        EskizSmsSender sender = new EskizSmsSender(props, client);

        sender.sendOtp("+998901234567", "789012", LanguageCode.RU);

        verify(client).sendSms(
                "+998901234567",
                "Код подтверждения RepairAuto: 789012. Код действует 5 минут. Никому его не сообщайте.");
    }

    @Test
    void sendOtp_englishLanguage_sendsEnglishTemplate() {
        EskizClient client = mock(EskizClient.class);
        SmsProperties props = SmsProperties.of(true, SmsProviderType.ESKIZ);
        EskizSmsSender sender = new EskizSmsSender(props, client);

        sender.sendOtp("+998901234567", "112233", LanguageCode.EN);

        verify(client).sendSms(
                "+998901234567",
                "Your RepairAuto verification code is: 112233. The code expires in 5 minutes. Do not share it with anyone.");
    }
}
