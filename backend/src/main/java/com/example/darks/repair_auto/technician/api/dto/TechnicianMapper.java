package com.example.darks.repair_auto.technician.api.dto;

import com.example.darks.repair_auto.repair.attachment.application.ImageAttachmentUtils;
import com.example.darks.repair_auto.technician.domain.Technician;

public final class TechnicianMapper {

    private TechnicianMapper() {
    }

    public static TechnicianSummaryResponse summary(Technician technician) {
        return new TechnicianSummaryResponse(
                technician.getId(),
                technician.getFullName(),
                technician.getPhone(),
                technician.getPhoneVerifiedAt() != null,
                technician.getEmail(),
                technician.getEmailVerifiedAt() != null,
                technician.getSpecialization(),
                technician.getMaximumConcurrentRequests(),
                technician.getPreferredLanguage(),
                technician.isActive(),
                technician.isTelegramLinked(),
                technician.getCreatedAt(),
                technician.getUpdatedAt(),
                ImageAttachmentUtils.toAvatarResponse(technician.getAvatarAttachment(), ImageAttachmentUtils.technicianAvatarDownloadUrl(technician.getId())));
    }

    public static TechnicianDetailResponse details(Technician technician) {
        return new TechnicianDetailResponse(
                technician.getId(),
                technician.getFullName(),
                technician.getPhone(),
                technician.getPhoneVerifiedAt() != null,
                technician.getEmail(),
                technician.getEmailVerifiedAt() != null,
                technician.getSpecialization(),
                technician.getNotes(),
                technician.getMaximumConcurrentRequests(),
                technician.getPreferredLanguage(),
                technician.isActive(),
                technician.isTelegramLinked(),
                technician.getCreatedAt(),
                technician.getUpdatedAt(),
                ImageAttachmentUtils.toAvatarResponse(technician.getAvatarAttachment(), ImageAttachmentUtils.technicianAvatarDownloadUrl(technician.getId())));
    }
}
