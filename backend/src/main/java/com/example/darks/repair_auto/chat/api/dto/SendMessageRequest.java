package com.example.darks.repair_auto.chat.api.dto;

import com.example.darks.repair_auto.chat.domain.ChatMessageType;

public record SendMessageRequest(
        Long conversationId,
        String clientMessageId,
        ChatMessageType type,
        String text,
        Long attachmentId,
        Long replyToMessageId
) {}
