package com.example.darks.repair_auto.shared.phone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import org.junit.jupiter.api.Test;

class PhoneNumberNormalizerTest {

    private final PhoneNumberNormalizer normalizer = new PhoneNumberNormalizer();

    @Test
    void givenSupportedUzbekInputsWhenNormalizingThenInternationalFormIsReturned() {
        assertThat(normalizer.normalize("+998 90 123 45 67")).isEqualTo("+998901234567");
        assertThat(normalizer.normalize("998901234567")).isEqualTo("+998901234567");
        assertThat(normalizer.normalize("90 123 45 67")).isEqualTo("+998901234567");
    }

    @Test
    void givenInvalidPhoneWhenNormalizingThenStableErrorIsReturned() {
        BusinessRuleException exception = catchThrowableOfType(
                () -> normalizer.normalize("12345"),
                BusinessRuleException.class);

        assertThat(exception.code()).isEqualTo("INVALID_PHONE_NUMBER");
        assertThat(exception.status()).isEqualTo(400);
    }
}
