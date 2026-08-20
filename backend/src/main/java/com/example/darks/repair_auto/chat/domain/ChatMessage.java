package com.example.darks.repair_auto.chat.domain;

import com.example.darks.repair_auto.identity.domain.ActorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 32)
    private ActorType senderType;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "client_message_id", nullable = false, length = 64)
    private String clientMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 32)
    private ChatMessageType messageType;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "attachment_id")
    private Long attachmentId;

    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "edited_at")
    private OffsetDateTime editedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected ChatMessage() {
    }

    public ChatMessage(
            Conversation conversation,
            ActorType senderType,
            Long senderId,
            String clientMessageId,
            ChatMessageType messageType,
            String text,
            Long attachmentId,
            Long replyToMessageId,
            OffsetDateTime createdAt) {
        this.conversation = Objects.requireNonNull(conversation, "conversation must not be null");
        this.senderType = Objects.requireNonNull(senderType, "senderType must not be null");
        this.senderId = Objects.requireNonNull(senderId, "senderId must not be null");
        this.clientMessageId = Objects.requireNonNull(clientMessageId, "clientMessageId must not be null");
        this.messageType = Objects.requireNonNull(messageType, "messageType must not be null");
        this.text = text;
        this.attachmentId = attachmentId;
        this.replyToMessageId = replyToMessageId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public Long getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public ActorType getSenderType() {
        return senderType;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getClientMessageId() {
        return clientMessageId;
    }

    public ChatMessageType getMessageType() {
        return messageType;
    }

    public String getText() {
        return text;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public Long getReplyToMessageId() {
        return replyToMessageId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getEditedAt() {
        return editedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete(OffsetDateTime now) {
        this.deletedAt = now;
    }
}
