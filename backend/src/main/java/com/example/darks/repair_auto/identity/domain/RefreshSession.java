package com.example.darks.repair_auto.identity.domain;

import com.example.darks.repair_auto.identity.domain.User;
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
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "token_family_id", nullable = false)
    private UUID tokenFamilyId;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "replaced_by_token_id")
    private Long replacedByTokenId;

    @Column(name = "revocation_reason", length = 120)
    private String revocationReason;

    @Column(name = "created_ip", length = 64)
    private String createdIp;

    @Column(name = "created_user_agent", length = 512)
    private String createdUserAgent;

    @Column(name = "last_used_ip", length = 64)
    private String lastUsedIp;

    @Column(name = "last_used_user_agent", length = 512)
    private String lastUsedUserAgent;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected RefreshSession() {
    }

    public RefreshSession(
            User user,
            String tokenHash,
            UUID tokenFamilyId,
            OffsetDateTime issuedAt,
            OffsetDateTime expiresAt,
            String createdIp,
            String createdUserAgent) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.tokenFamilyId = tokenFamilyId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.createdAt = issuedAt;
        this.createdIp = createdIp;
        this.createdUserAgent = createdUserAgent;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getTokenFamilyId() {
        return tokenFamilyId;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(OffsetDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public void markUsed(OffsetDateTime now, String ip, String userAgent) {
        this.usedAt = now;
        this.lastUsedIp = ip;
        this.lastUsedUserAgent = userAgent;
    }

    public void replaceWith(Long replacementId) {
        this.replacedByTokenId = replacementId;
    }

    public void revoke(OffsetDateTime now, String reason) {
        if (this.revokedAt == null) {
            this.revokedAt = now;
            this.revocationReason = reason;
        }
    }
}
