package com.example.darks.repair_auto.realtime.event.application;

import com.example.darks.repair_auto.identity.domain.ActorType;

public record ParticipantRecipient(
        ActorType actorType,
        Long actorId
) {}
