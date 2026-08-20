package com.example.darks.repair_auto.chat.api.dto;

import com.example.darks.repair_auto.chat.domain.ChatMessage;
import com.example.darks.repair_auto.chat.domain.Conversation;
import com.example.darks.repair_auto.chat.domain.ConversationParticipant;
import com.example.darks.repair_auto.realtime.event.dto.ChatMessagePayload;
import java.util.List;

public final class ChatMapper {

    private ChatMapper() {
    }

    public static ChatMessageResponse toResponse(ChatMessage message) {
        if (message == null) {
            return null;
        }
        return new ChatMessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSenderType(),
                message.getSenderId(),
                message.getClientMessageId(),
                message.getMessageType(),
                message.getText(),
                message.getAttachmentId(),
                message.getReplyToMessageId(),
                message.getCreatedAt(),
                message.getEditedAt(),
                message.getDeletedAt());
    }

    public static ChatMessagePayload toPayload(ChatMessage message) {
        if (message == null) {
            return null;
        }
        return new ChatMessagePayload(
                message.getId(),
                message.getConversation().getId(),
                message.getSenderType().name(),
                message.getSenderId(),
                message.getClientMessageId(),
                message.getMessageType().name(),
                message.getText(),
                message.getAttachmentId(),
                message.getReplyToMessageId(),
                message.getCreatedAt().toInstant());
    }

    public static ParticipantSummaryResponse toParticipantSummary(
            ConversationParticipant participant,
            String displayName) {
        if (participant == null) {
            return null;
        }
        return new ParticipantSummaryResponse(
                participant.getId(),
                participant.getActorType(),
                participant.getActorId(),
                participant.getRole(),
                displayName,
                participant.getLastReadMessageId(),
                participant.getLastReadAt(),
                participant.isActive());
    }

    public static ConversationSummaryResponse toSummary(
            Conversation conversation,
            long unreadCount,
            ChatMessage lastMessage,
            List<ParticipantSummaryResponse> participants) {
        String requestNumber = conversation.getRepairRequest() != null
                ? conversation.getRepairRequest().getRequestNumber()
                : null;
        Long requestId = conversation.getRepairRequest() != null
                ? conversation.getRepairRequest().getId()
                : null;

        return new ConversationSummaryResponse(
                conversation.getId(),
                requestId,
                requestNumber,
                conversation.getConversationType(),
                conversation.getStatus(),
                unreadCount,
                toResponse(lastMessage),
                participants,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }

    public static ConversationDetailResponse toDetails(
            Conversation conversation,
            long unreadCount,
            List<ParticipantSummaryResponse> participants) {
        String requestNumber = conversation.getRepairRequest() != null
                ? conversation.getRepairRequest().getRequestNumber()
                : null;
        Long requestId = conversation.getRepairRequest() != null
                ? conversation.getRepairRequest().getId()
                : null;

        return new ConversationDetailResponse(
                conversation.getId(),
                requestId,
                requestNumber,
                conversation.getConversationType(),
                conversation.getStatus(),
                unreadCount,
                participants,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                conversation.getClosedAt());
    }
}
