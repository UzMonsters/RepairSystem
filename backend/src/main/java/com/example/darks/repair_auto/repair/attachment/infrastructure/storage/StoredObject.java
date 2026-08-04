package com.example.darks.repair_auto.repair.attachment.infrastructure.storage;

public record StoredObject(
        String storageKey,
        String contentType,
        long sizeBytes
) {
}
