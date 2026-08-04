package com.example.darks.repair_auto.repair.attachment.infrastructure.storage;

import java.io.InputStream;

public record StorageUpload(
        String storageKey,
        String contentType,
        long sizeBytes,
        InputStream inputStream
) {
}
