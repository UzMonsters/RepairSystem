package com.example.darks.repair_auto.realtime.event.dto;

public record ChatTypingPayload(
        Long conversationId,
        String actorType,
        Long actorId,
        boolean typing
) {}
