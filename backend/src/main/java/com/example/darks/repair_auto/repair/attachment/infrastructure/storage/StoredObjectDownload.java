package com.example.darks.repair_auto.repair.attachment.infrastructure.storage;

import java.io.InputStream;

public record StoredObjectDownload(
        String contentType,
        long sizeBytes,
        InputStream inputStream
) {
}
