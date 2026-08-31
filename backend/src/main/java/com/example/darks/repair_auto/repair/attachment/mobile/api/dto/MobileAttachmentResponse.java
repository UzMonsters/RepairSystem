package com.example.darks.repair_auto.repair.attachment.mobile.api.dto;

import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Mobile client attachment summary")
public record MobileAttachmentResponse(
        @Schema(description = "Attachment identifier", example = "501")
        Long id,

        @Schema(description = "Repair request identifier", example = "42")
        Long repairRequestId,

        @Schema(description = "Attachment business type", example = "CUSTOMER_PROBLEM_PHOTO")
        AttachmentType type,

        @Schema(description = "Original uploaded filename", example = "air-conditioner-fault.jpg")
        String originalFileName,

        @Schema(description = "MIME content type", example = "image/jpeg")
        String contentType,

        @Schema(description = "File size in bytes", example = "1834421")
        Long sizeBytes,

        @Schema(description = "Attachment lifecycle status", example = "AVAILABLE")
        AttachmentStatus status,

        @Schema(description = "API-relative download URL", example = "/api/v1/mobile/me/attachments/501/download")
        String downloadUrl,

        @Schema(description = "Whether the file is an image previewable inline", example = "true")
        Boolean imagePreview,

        @Schema(description = "Upload timestamp", example = "2026-08-18T10:30:00Z")
        OffsetDateTime uploadedAt
) {
    public MobileAttachmentResponse(
            Long id,
            Long repairRequestId,
            AttachmentType type,
            String originalFileName,
            String contentType,
            Long sizeBytes,
            AttachmentStatus status,
            OffsetDateTime uploadedAt
    ) {
        this(id, repairRequestId, type, originalFileName, contentType, sizeBytes, status, null, null, uploadedAt);
    }
}
