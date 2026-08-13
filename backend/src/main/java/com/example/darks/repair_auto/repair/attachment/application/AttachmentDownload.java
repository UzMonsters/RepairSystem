package com.example.darks.repair_auto.repair.attachment.application;

import java.io.InputStream;

public record AttachmentDownload(
        String fileName,
        String contentType,
        long sizeBytes,
        InputStream inputStream
) {
}
