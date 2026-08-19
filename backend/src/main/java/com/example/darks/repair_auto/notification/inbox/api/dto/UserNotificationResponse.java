package com.example.darks.repair_auto.notification.inbox.api.dto;

import com.example.darks.repair_auto.notification.domain.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Mobile in-app notification item")
public record UserNotificationResponse(
        @Schema(description = "Unique notification ID", example = "501")
        Long id,

        @Schema(description = "Notification business event type", example = "REPAIR_COMPLETED")
        NotificationType type,

        @Schema(description = "Rendered localized notification title", example = "Ta'mirlash yakunlandi")
        String title,

        @Schema(description = "Rendered localized notification body text", example = "REQ-2026-000042 buyurtmangiz muvaffaqiyatli yakunlandi.")
        String body,

        @Schema(description = "Whether the notification has been marked as read", example = "false")
        boolean read,

        @Schema(description = "Timestamp when the notification was marked as read", example = "2026-08-18T11:15:00Z")
        OffsetDateTime readAt,

        @Schema(description = "Semantic navigation target in the mobile app", example = "REPAIR_REQUEST_DETAILS")
        String target,

        @Schema(description = "Target resource ID for semantic navigation", example = "101")
        Long targetId,

        @Schema(description = "Human-readable repair request number", example = "REQ-2026-000042")
        String requestNumber,

        @Schema(description = "Timestamp when the notification was created", example = "2026-08-18T11:10:00Z")
        OffsetDateTime createdAt
) {
}
