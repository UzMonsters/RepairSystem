package com.example.darks.repair_auto.chat.api.dto;

import com.example.darks.repair_auto.identity.domain.ActorType;
import java.time.OffsetDateTime;

public record ParticipantSummaryResponse(
        Long id,
        ActorType actorType,
        Long actorId,
        String role,
        String displayName,
        Long lastReadMessageId,
        OffsetDateTime lastReadAt,
        boolean active
) {}
