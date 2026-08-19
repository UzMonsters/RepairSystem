package com.example.darks.repair_auto.repair.attachment.mobile.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Short-lived temporary presigned download URL for mobile attachment")
public record MobileAttachmentDownloadUrlResponse(
        @Schema(description = "Attachment identifier", example = "501")
        Long attachmentId,

        @Schema(description = "Temporary presigned download URL", example = "https://s3.example.com/bucket/key?signature=...")
        String url,

        @Schema(description = "Presigned URL expiration timestamp", example = "2026-08-18T11:00:00Z")
        OffsetDateTime expiresAt
) {
}
