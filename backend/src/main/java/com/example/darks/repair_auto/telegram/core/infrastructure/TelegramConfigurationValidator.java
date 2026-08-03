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
        if (properties.getBotToken().isBlank()) {
            throw new IllegalStateException("Telegram bot token is required when Telegram is enabled.");
        }
        if (properties.getWebhookSecret().isBlank()) {
            throw new IllegalStateException("Telegram webhook secret is required when Telegram is enabled.");
        }
    }
}
