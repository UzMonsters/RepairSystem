package com.example.darks.repair_auto.chat.api.dto;

import com.example.darks.repair_auto.chat.domain.ConversationStatus;
import com.example.darks.repair_auto.chat.domain.ConversationType;
import java.time.OffsetDateTime;
import java.util.List;

public record ConversationDetailResponse(
        Long id,
        Long repairRequestId,
        String requestNumber,
        ConversationType conversationType,
        ConversationStatus status,
        long unreadCount,
        List<ParticipantSummaryResponse> participants,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime closedAt
) {}
