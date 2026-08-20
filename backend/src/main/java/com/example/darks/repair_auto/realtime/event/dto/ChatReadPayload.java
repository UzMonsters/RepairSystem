package com.example.darks.repair_auto.realtime.event.dto;

import java.time.Instant;

public record ChatReadPayload(
        Long conversationId,
        Long messageId,
        String readerType,
        Long readerId,
        Instant readAt
) {}
