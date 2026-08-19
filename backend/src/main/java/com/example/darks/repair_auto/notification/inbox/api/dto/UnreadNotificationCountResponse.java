package com.example.darks.repair_auto.notification.inbox.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unread notification count for the authenticated actor")
public record UnreadNotificationCountResponse(
        @Schema(description = "Total number of unread notifications", example = "3")
        long unreadCount
) {
}
