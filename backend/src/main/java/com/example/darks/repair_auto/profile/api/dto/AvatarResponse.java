package com.example.darks.repair_auto.profile.api.dto;

public record AvatarResponse(
        Long attachmentId,
        String fileName,
        String contentType,
        String url
) {
}
