package com.example.darks.repair_auto.repair.attachment.api.dto;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentTechnicianSummary;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestUserSummary;
import com.example.darks.repair_auto.technician.domain.Technician;

public final class AttachmentMapper {

    private AttachmentMapper() {
    }

    public static AttachmentResponse response(RepairAttachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getRepairRequest().getId(),
                attachment.getAttachmentType(),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getStatus(),
                user(attachment.getUploadedByUser()),
                technician(attachment.getUploadedByTechnician()),
                attachment.getUploadedAt());
    }

    private static RepairRequestUserSummary user(User user) {
        if (user == null) {
            return null;
        }
        return new RepairRequestUserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    private static AssignmentTechnicianSummary technician(Technician technician) {
        if (technician == null) {
            return null;
        }
        return new AssignmentTechnicianSummary(
                technician.getId(),
                technician.getFullName(),
                technician.getPhone(),
                technician.isActive());
    }
}
