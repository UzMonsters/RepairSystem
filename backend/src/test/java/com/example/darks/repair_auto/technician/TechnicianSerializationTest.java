package com.example.darks.repair_auto.technician;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class TechnicianSerializationTest {

    @Test
    void givenTechnicianEntityWhenSerializedThenInternalTelegramAndVersionFieldsAreNotEmitted() throws Exception {
        Technician technician = new Technician(
                "Alex",
                "+998902223344",
                "AC",
                "Notes",
                5,
                LanguageCode.UZ,
                true,
                OffsetDateTime.parse("2026-01-01T00:00:00Z"));

        String json = new ObjectMapper().writeValueAsString(technician);

        assertThat(json).doesNotContain("telegramUserId");
        assertThat(json).doesNotContain("telegramChatId");
        assertThat(json).doesNotContain("telegramLinkedAt");
        assertThat(json).doesNotContain("version");
    }
}
