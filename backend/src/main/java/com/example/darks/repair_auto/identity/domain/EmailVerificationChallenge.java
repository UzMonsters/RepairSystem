package com.example.darks.repair_auto.identity.domain;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.technician.domain.Technician;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_verification_challenges")
public class EmailVerificationChallenge {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 32)
    private ActorType actorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @Column(name = "pending_email", nullable = false, length = 254)
    private String pendingEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private EmailVerificationPurpose purpose;

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

    @Version
    @Column(nullable = false)
    private long version;

    protected EmailVerificationChallenge() {
    }

    private EmailVerificationChallenge(
            ActorType actorType,
            Customer customer,
            Technician technician,
            String pendingEmail,
            EmailVerificationPurpose purpose,
            String codeHash,
            int maxAttempts,
            OffsetDateTime now,
            OffsetDateTime expiresAt,
            OffsetDateTime resendAvailableAt) {
        this.id = UUID.randomUUID();
        this.actorType = actorType;
        this.customer = customer;
        this.technician = technician;
        this.pendingEmail = pendingEmail;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.maxAttempts = maxAttempts;
        this.createdAt = now;
        this.expiresAt = expiresAt;
        this.resendAvailableAt = resendAvailableAt;
    }

    public static EmailVerificationChallenge forCustomer(
            Customer customer,
            String pendingEmail,
            String codeHash,
            int maxAttempts,
            OffsetDateTime now,
            OffsetDateTime expiresAt,
            OffsetDateTime resendAvailableAt) {
        return new EmailVerificationChallenge(
                ActorType.CUSTOMER,
                customer,
                null,
                pendingEmail,
                EmailVerificationPurpose.CHANGE_EMAIL,
                codeHash,
                maxAttempts,
                now,
                expiresAt,
                resendAvailableAt);
    }

    public static EmailVerificationChallenge forTechnician(
            Technician technician,
            String pendingEmail,
            String codeHash,
            int maxAttempts,
            OffsetDateTime now,
            OffsetDateTime expiresAt,
            OffsetDateTime resendAvailableAt) {
        return new EmailVerificationChallenge(
                ActorType.TECHNICIAN,
                null,
                technician,
                pendingEmail,
                EmailVerificationPurpose.CHANGE_EMAIL,
                codeHash,
                maxAttempts,
                now,
                expiresAt,
                resendAvailableAt);
    }

    public UUID getId() {
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

    public String getPendingEmail() {
        return pendingEmail;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public boolean isExpired(OffsetDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getResendAvailableAt() {
        return resendAvailableAt;
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
