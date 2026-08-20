package com.example.darks.repair_auto.realtime.event.application;

import com.example.darks.repair_auto.identity.domain.ActorType;

public record NotificationReadDomainEvent(
        ActorType recipientType,
        Long recipientId,
        Long notificationId
) {}
