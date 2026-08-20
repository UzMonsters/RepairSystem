package com.example.darks.repair_auto.realtime.event.dto;

import java.time.Instant;

public record ChatMessagePayload(
        Long messageId,
        Long conversationId,
        String senderType,
        Long senderId,
        String clientMessageId,
        String messageType,
        String text,
        Long attachmentId,
        Long replyToMessageId,
        Instant createdAt
) {}
