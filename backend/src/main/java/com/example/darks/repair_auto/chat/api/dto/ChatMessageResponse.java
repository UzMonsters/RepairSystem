package com.example.darks.repair_auto.chat.api.dto;

import com.example.darks.repair_auto.chat.domain.ChatMessageType;
import com.example.darks.repair_auto.identity.domain.ActorType;
import java.time.OffsetDateTime;

public record ChatMessageResponse(
        Long id,
        Long conversationId,
        ActorType senderType,
        Long senderId,
        String clientMessageId,
        ChatMessageType messageType,
        String text,
        Long attachmentId,
        Long replyToMessageId,
        OffsetDateTime createdAt,
        OffsetDateTime editedAt,
        OffsetDateTime deletedAt
) {}
