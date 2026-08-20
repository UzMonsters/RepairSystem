package com.example.darks.repair_auto.realtime.event.application;

import com.example.darks.repair_auto.identity.domain.ActorType;

public record NotificationCreatedDomainEvent(
        ActorType recipientType,
        Long recipientId,
        Long notificationId,
        String notificationType,
        Long targetId,
        String target
) {}
