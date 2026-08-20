package com.example.darks.repair_auto.realtime.event.application;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.realtime.event.dto.ChatReadPayload;
import java.util.List;

public record ChatMessageReadDomainEvent(
        Long conversationId,
        Long messageId,
        ActorType readerType,
        Long readerId,
        List<ParticipantRecipient> recipients,
        ChatReadPayload payload
) {}
