package com.example.darks.repair_auto.identity.domain;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.technician.domain.Technician;
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
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mobile_refresh_sessions")
public class MobileRefreshSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 32)
    private ActorType actorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "token_family_id", nullable = false)
    private UUID tokenFamilyId;

    @Column(name = "parent_session_id")
    private Long parentSessionId;

    @Column(name = "replaced_by_session_id")
    private Long replacedBySessionId;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revocation_reason", length = 64)
    private String revocationReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected MobileRefreshSession() {
    }

    public MobileRefreshSession(
            ActorType actorType,
            Customer customer,
            Technician technician,
            String tokenHash,
            UUID tokenFamilyId,
            Long parentSessionId,
            OffsetDateTime issuedAt,
            OffsetDateTime expiresAt) {
        if (actorType == ActorType.CUSTOMER) {
            if (customer == null || technician != null) {
                throw new IllegalArgumentException("Customer session must have customer and null technician");
            }
        } else if (actorType == ActorType.TECHNICIAN) {
            if (technician == null || customer != null) {
                throw new IllegalArgumentException("Technician session must have technician and null customer");
            }
        } else {
            throw new IllegalArgumentException("Unsupported actor type for mobile refresh session: " + actorType);
        }
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("tokenHash must not be blank");
        }
        if (tokenFamilyId == null) {
            throw new IllegalArgumentException("tokenFamilyId must not be null");
        }
        if (issuedAt == null || expiresAt == null || !expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }

        this.actorType = actorType;
        this.customer = customer;
        this.technician = technician;
        this.tokenHash = tokenHash;
        this.tokenFamilyId = tokenFamilyId;
        this.parentSessionId = parentSessionId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.createdAt = issuedAt;
        this.updatedAt = issuedAt;
    }

    public static MobileRefreshSession forCustomer(
            Customer customer,
            String tokenHash,
            UUID tokenFamilyId,
            Long parentSessionId,
            OffsetDateTime issuedAt,
            OffsetDateTime expiresAt) {
        return new MobileRefreshSession(
                ActorType.CUSTOMER,
                customer,
                null,
                tokenHash,
                tokenFamilyId,
                parentSessionId,
                issuedAt,
                expiresAt);
    }

    public static MobileRefreshSession forTechnician(
            Technician technician,
            String tokenHash,
            UUID tokenFamilyId,
            Long parentSessionId,
            OffsetDateTime issuedAt,
            OffsetDateTime expiresAt) {
        return new MobileRefreshSession(
                ActorType.TECHNICIAN,
                null,
                technician,
                tokenHash,
                tokenFamilyId,
                parentSessionId,
                issuedAt,
                expiresAt);
    }

    public Long getId() {
        return id;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Technician getTechnician() {
        return technician;
    }

    public Long getActorId() {
        return actorType == ActorType.CUSTOMER ? customer.getId() : technician.getId();
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getTokenFamilyId() {
        return tokenFamilyId;
    }

    public Long getParentSessionId() {
        return parentSessionId;
    }

    public Long getReplacedBySessionId() {
        return replacedBySessionId;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public boolean isUsed() {
        return lastUsedAt != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(OffsetDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public void markUsed(OffsetDateTime now) {
        this.lastUsedAt = now;
        this.updatedAt = now;
    }

    public void replaceWith(Long replacementId, OffsetDateTime now) {
        this.replacedBySessionId = replacementId;
        this.updatedAt = now;
    }

    public void revoke(OffsetDateTime now, String reason) {
        if (this.revokedAt == null) {
            this.revokedAt = now;
            this.revocationReason = reason;
            this.updatedAt = now;
        }
    }
}
