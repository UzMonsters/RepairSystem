package com.example.darks.repair_auto.realtime.event.dto;

public record NotificationEventPayload(
        Long notificationId,
        String notificationType,
        Long targetId,
        String target,
        boolean read
) {}
