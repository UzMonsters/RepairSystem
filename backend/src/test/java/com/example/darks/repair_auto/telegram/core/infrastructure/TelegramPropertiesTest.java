package com.example.darks.repair_auto.telegram.core.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelegramPropertiesTest {

    @Test
    void givenBotUsernameWithAtSignWhenConfiguredThenUsernameIsNormalized() {
        TelegramProperties.Bot bot = new TelegramProperties.Bot();

        bot.setBotUsername(" @RepairAutoStaffTestBot ");

        assertThat(bot.getBotUsername()).isEqualTo("RepairAutoStaffTestBot");
    }
}
