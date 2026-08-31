package com.example.darks.repair_auto.profile.api.dto;

import java.time.OffsetDateTime;

public record AvatarResponse(
        Long attachmentId,
        String fileName,
        String contentType,
        Long sizeBytes,
        String downloadUrl,
        OffsetDateTime uploadedAt
) {
}
