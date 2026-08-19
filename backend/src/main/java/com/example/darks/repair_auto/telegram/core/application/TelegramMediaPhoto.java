package com.example.darks.repair_auto.telegram.core.application;

public record TelegramMediaPhoto(
        String filename,
        byte[] bytes,
        String caption
) {
    public TelegramMediaPhoto(String filename, byte[] bytes) {
        this(filename, bytes, null);
    }
}
