package com.example.darks.repair_auto.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CustomerSerializationTest {

    @Test
    void givenCustomerEntityWhenSerializedThenInternalTelegramAndVersionFieldsAreNotEmitted() throws Exception {
        Customer customer = new Customer(
                "Ali",
                "+998901112233",
                LanguageCode.UZ,
                OffsetDateTime.parse("2026-01-01T00:00:00Z"));

        String json = new ObjectMapper().writeValueAsString(customer);

        assertThat(json).doesNotContain("telegramUserId");
        assertThat(json).doesNotContain("telegramChatId");
        assertThat(json).doesNotContain("version");
    }
}
