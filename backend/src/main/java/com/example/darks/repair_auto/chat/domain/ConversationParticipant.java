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
@Table(name = "conversation_participants")
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 32)
    private ActorType actorType;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "role", length = 32)
    private String role;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "last_read_at")
    private OffsetDateTime lastReadAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ConversationParticipant() {
    }

    public ConversationParticipant(
            Conversation conversation,
            ActorType actorType,
            Long actorId,
            String role,
            OffsetDateTime now) {
        this.conversation = Objects.requireNonNull(conversation, "conversation must not be null");
        this.actorType = Objects.requireNonNull(actorType, "actorType must not be null");
        this.actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        this.role = role;
        this.joinedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getRole() {
        return role;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public OffsetDateTime getLeftAt() {
        return leftAt;
    }

    public Long getLastReadMessageId() {
        return lastReadMessageId;
    }

    public OffsetDateTime getLastReadAt() {
        return lastReadAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return leftAt == null;
    }

    public void leave(OffsetDateTime now) {
        this.leftAt = now;
        this.updatedAt = now;
    }

    public void rejoin(OffsetDateTime now) {
        this.leftAt = null;
        this.joinedAt = now;
        this.updatedAt = now;
    }

    public boolean advanceReadState(Long messageId, OffsetDateTime now) {
        if (messageId == null) {
            return false;
        }
        if (this.lastReadMessageId == null || messageId > this.lastReadMessageId) {
            this.lastReadMessageId = messageId;
            this.lastReadAt = now;
            this.updatedAt = now;
            return true;
        }
        return false;
    }
}
