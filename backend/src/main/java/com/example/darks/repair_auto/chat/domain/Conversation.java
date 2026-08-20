package com.example.darks.repair_auto.chat.domain;

import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repair_request_id")
    private RepairRequest repairRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false, length = 32)
    private ConversationType conversationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ConversationStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConversationParticipant> participants = new ArrayList<>();

    protected Conversation() {
    }

    public Conversation(
            RepairRequest repairRequest,
            ConversationType conversationType,
            OffsetDateTime now) {
        this.repairRequest = repairRequest;
        this.conversationType = conversationType;
        this.status = ConversationStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public RepairRequest getRepairRequest() {
        return repairRequest;
    }

    public ConversationType getConversationType() {
        return conversationType;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public List<ConversationParticipant> getParticipants() {
        return participants;
    }

    public void touch(OffsetDateTime now) {
        this.updatedAt = now;
    }

    public void close(OffsetDateTime now) {
        this.status = ConversationStatus.CLOSED;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return this.status == ConversationStatus.ACTIVE;
    }
}
