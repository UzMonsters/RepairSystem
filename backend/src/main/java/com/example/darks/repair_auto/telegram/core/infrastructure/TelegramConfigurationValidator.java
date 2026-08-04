package com.example.darks.repair_auto.telegram.core.infrastructure;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

@Component
public class TelegramConfigurationValidator implements SmartInitializingSingleton {

    private final TelegramProperties properties;

    public TelegramConfigurationValidator(TelegramProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.isEnabled()) {
            return;
        }
        requireBot(properties.getCustomer(), "customer");
        requireBot(properties.getTechnician(), "technician");
    }

    private void requireBot(TelegramProperties.Bot bot, String name) {
        if (bot.getBotToken().isBlank()) {
            throw new IllegalStateException("Telegram " + name + " bot token is required when Telegram is enabled.");
        }
        if (bot.getWebhookSecret().isBlank()) {
            throw new IllegalStateException("Telegram " + name + " webhook secret is required when Telegram is enabled.");
        }
    }
}
