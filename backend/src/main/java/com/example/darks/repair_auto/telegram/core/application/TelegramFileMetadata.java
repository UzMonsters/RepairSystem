package com.example.darks.repair_auto.telegram.core.application;

public record TelegramFileMetadata(
        String fileId,
        String filePath,
        long fileSize) {
}
