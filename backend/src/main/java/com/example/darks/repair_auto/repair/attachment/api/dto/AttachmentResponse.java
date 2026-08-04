package com.example.darks.repair_auto.repair.attachment.api.dto;

import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentTechnicianSummary;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestUserSummary;
import java.time.OffsetDateTime;

public record AttachmentResponse(
        Long id,
        Long repairRequestId,
        AttachmentType type,
        String originalFileName,
        String contentType,
        Long sizeBytes,
        AttachmentStatus status,
        RepairRequestUserSummary uploadedBy,
        AssignmentTechnicianSummary uploadedByTechnician,
        OffsetDateTime uploadedAt
) {
}
