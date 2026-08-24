package com.example.darks.repair_auto.identity.domain;

import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "phone_otp_challenges")
public class PhoneOtpChallenge {

    @Id
    private UUID id;

    @Column(nullable = false, length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 32)
    private ActorType actorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 32)
    private PushClientType clientType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private PhoneOtpPurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 128)
    private String codeHash;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "resend_available_at", nullable = false)
    private OffsetDateTime resendAvailableAt;

    @Column(name = "request_ip", length = 64)
    private String requestIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Version
    @Column(nullable = false)
    private long version;

    protected PhoneOtpChallenge() {
    }

    public PhoneOtpChallenge(
            String phone,
            ActorType actorType,
            PushClientType clientType,
            PhoneOtpPurpose purpose,
            String codeHash,
            int maxAttempts,
            OffsetDateTime now,
            OffsetDateTime expiresAt,
            OffsetDateTime resendAvailableAt,
            String requestIp,
            String userAgent) {
        this.id = UUID.randomUUID();
        this.phone = phone;
        this.actorType = actorType;
        this.clientType = clientType;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.maxAttempts = maxAttempts;
        this.createdAt = now;
        this.expiresAt = expiresAt;
        this.resendAvailableAt = resendAvailableAt;
        this.requestIp = requestIp;
        this.userAgent = userAgent;
    }

    public UUID getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public PushClientType getClientType() {
        return clientType;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public PhoneOtpPurpose getPurpose() {
        return purpose;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getResendAvailableAt() {
        return resendAvailableAt;
    }

    public boolean isExpired(OffsetDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean hasAttemptsRemaining() {
        return attemptCount < maxAttempts;
    }

    public void failAttempt() {
        this.attemptCount++;
    }

    public void consume(OffsetDateTime now) {
        this.consumedAt = now;
    }
}
