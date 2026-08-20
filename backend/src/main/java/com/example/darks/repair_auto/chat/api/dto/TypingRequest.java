package com.example.darks.repair_auto.chat.api.dto;

public record TypingRequest(
        Long conversationId,
        Boolean typing
) {}
