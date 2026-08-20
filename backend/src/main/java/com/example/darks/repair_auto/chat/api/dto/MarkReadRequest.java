package com.example.darks.repair_auto.chat.api.dto;

public record MarkReadRequest(
        Long conversationId,
        Long messageId
) {}
