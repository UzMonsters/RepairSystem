package com.example.darks.repair_auto.identity.mobile.telegram;

public record TelegramIdentity(
        Long telegramUserId,
        String subject,
        String name,
        String username,
        String phoneNumber
) {
    public TelegramIdentity {
        if (telegramUserId == null || telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be a positive number.");
        }
    }
}
