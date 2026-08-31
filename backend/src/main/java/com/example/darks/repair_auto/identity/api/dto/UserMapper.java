package com.example.darks.repair_auto.identity.api.dto;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.repair.attachment.application.ImageAttachmentUtils;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserSummaryResponse summary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                ImageAttachmentUtils.toAvatarResponse(user.getAvatarAttachment(), ImageAttachmentUtils.staffAvatarDownloadUrl(user.getId())));
    }

    public static UserDetailsResponse details(User user) {
        return new UserDetailsResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                ImageAttachmentUtils.toAvatarResponse(user.getAvatarAttachment(), ImageAttachmentUtils.staffAvatarDownloadUrl(user.getId())));
    }
}
