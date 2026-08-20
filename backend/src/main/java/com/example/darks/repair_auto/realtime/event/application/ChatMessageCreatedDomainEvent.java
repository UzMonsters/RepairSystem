package com.example.darks.repair_auto.realtime.event.application;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.realtime.event.dto.ChatMessagePayload;
import java.util.List;

public record ChatMessageCreatedDomainEvent(
        Long conversationId,
        Long messageId,
        ActorType senderType,
        Long senderId,
        List<ParticipantRecipient> recipients,
        ChatMessagePayload payload
) {}
