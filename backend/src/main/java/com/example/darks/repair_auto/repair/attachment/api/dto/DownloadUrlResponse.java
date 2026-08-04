package com.example.darks.repair_auto.repair.attachment.api.dto;

import java.time.OffsetDateTime;

public record DownloadUrlResponse(
        String url,
        OffsetDateTime expiresAt
) {
}
