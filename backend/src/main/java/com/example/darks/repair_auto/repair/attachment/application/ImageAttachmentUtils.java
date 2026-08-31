package com.example.darks.repair_auto.repair.attachment.application;

import com.example.darks.repair_auto.profile.api.dto.AvatarResponse;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import java.util.Locale;

public final class ImageAttachmentUtils {

    private ImageAttachmentUtils() {
    }

    public static boolean isImagePreviewable(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("image/jpeg")
                || normalized.equals("image/png")
                || normalized.equals("image/webp")
                || normalized.equals("image/gif");
    }

    public static AvatarResponse toAvatarResponse(RepairAttachment attachment, String downloadUrl) {
        if (attachment == null || !attachment.isAvailable()) {
            return null;
        }
        return new AvatarResponse(
                attachment.getId(),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                downloadUrl,
                attachment.getUploadedAt());
    }

    public static String staffAvatarDownloadUrl(Long userId) {
        return "/api/v1/users/" + userId + "/avatar";
    }

    public static String currentStaffAvatarDownloadUrl() {
        return "/api/v1/me/avatar";
    }

    public static String customerAvatarDownloadUrl(Long customerId) {
        return "/api/v1/customers/" + customerId + "/avatar";
    }

    public static String technicianAvatarDownloadUrl(Long technicianId) {
        return "/api/v1/technicians/" + technicianId + "/avatar";
    }

    public static String requestAttachmentDownloadUrl(Long attachmentId) {
        return "/api/v1/attachments/" + attachmentId + "/download";
    }

    public static String mobileSelfAvatarDownloadUrl() {
        return "/api/v1/mobile/me/avatar";
    }

    public static String mobileAttachmentDownloadUrl(Long attachmentId) {
        return "/api/v1/mobile/me/attachments/" + attachmentId + "/download";
    }
}
