package com.example.darks.repair_auto.telegram.technician.domain;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.technician.domain.Technician;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;

@Entity
@Table(name = "telegram_technician_link_tokens")
public class TelegramTechnicianLinkToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private Technician technician;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "used_by_telegram_user_id")
    private Long usedByTelegramUserId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected TelegramTechnicianLinkToken() {
    }

    public TelegramTechnicianLinkToken(
            String tokenHash,
            Technician technician,
            User createdByUser,
            OffsetDateTime expiresAt,
            OffsetDateTime now) {
        this.tokenHash = tokenHash;
        this.technician = technician;
        this.createdByUser = createdByUser;
        this.expiresAt = expiresAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Technician getTechnician() {
        return technician;
    }

    public boolean isUsable(OffsetDateTime now) {
        return usedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }

    public void used(Long telegramUserId, OffsetDateTime now) {
        this.usedAt = now;
        this.usedByTelegramUserId = telegramUserId;
        this.updatedAt = now;
    }

    public void revoked(OffsetDateTime now) {
        this.revokedAt = now;
        this.updatedAt = now;
    }
}
