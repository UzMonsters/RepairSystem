package com.example.darks.repair_auto.repair.attachment.api.dto;

import jakarta.validation.constraints.Size;

public record AttachmentDeleteRequest(
        @Size(max = 1000) String reason
) {
}
