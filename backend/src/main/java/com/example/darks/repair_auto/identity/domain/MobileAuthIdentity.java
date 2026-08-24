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

@Entity
@Table(name = "mobile_auth_identities")
public class MobileAuthIdentity {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MobileAuthProvider provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "provider_email", length = 254)
    private String providerEmail;

    @Column(name = "provider_phone", length = 32)
    private String providerPhone;

    @Column(name = "verified_at", nullable = false)
    private OffsetDateTime verifiedAt;

    @Column(name = "linked_at", nullable = false)
    private OffsetDateTime linkedAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "disabled_at")
    private OffsetDateTime disabledAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected MobileAuthIdentity() {
    }

    private MobileAuthIdentity(
            ActorType actorType,
            Customer customer,
            Technician technician,
            MobileAuthProvider provider,
            String providerSubject,
            String providerEmail,
            String providerPhone,
            OffsetDateTime now) {
        if (actorType == ActorType.CUSTOMER && (customer == null || technician != null)) {
            throw new IllegalArgumentException("Customer identity must have customer only");
        }
        if (actorType == ActorType.TECHNICIAN && (technician == null || customer != null)) {
            throw new IllegalArgumentException("Technician identity must have technician only");
        }
        this.actorType = actorType;
        this.customer = customer;
        this.technician = technician;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.providerEmail = providerEmail;
        this.providerPhone = providerPhone;
        this.verifiedAt = now;
        this.linkedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static MobileAuthIdentity forCustomer(
            Customer customer,
            MobileAuthProvider provider,
            String providerSubject,
            String providerEmail,
            String providerPhone,
            OffsetDateTime now) {
        return new MobileAuthIdentity(
                ActorType.CUSTOMER,
                customer,
                null,
                provider,
                providerSubject,
                providerEmail,
                providerPhone,
                now);
    }

    public static MobileAuthIdentity forTechnician(
            Technician technician,
            MobileAuthProvider provider,
            String providerSubject,
            String providerEmail,
            String providerPhone,
            OffsetDateTime now) {
        return new MobileAuthIdentity(
                ActorType.TECHNICIAN,
                null,
                technician,
                provider,
                providerSubject,
                providerEmail,
                providerPhone,
                now);
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

    public MobileAuthProvider getProvider() {
        return provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public String getProviderEmail() {
        return providerEmail;
    }

    public String getProviderPhone() {
        return providerPhone;
    }

    public OffsetDateTime getLinkedAt() {
        return linkedAt;
    }

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public OffsetDateTime getDisabledAt() {
        return disabledAt;
    }

    public boolean isActive() {
        return disabledAt == null;
    }

    public void markUsed(OffsetDateTime now) {
        this.lastUsedAt = now;
        this.updatedAt = now;
    }

    public void updatePhone(String phone, OffsetDateTime now) {
        this.providerSubject = phone;
        this.providerPhone = phone;
        this.verifiedAt = now;
        this.updatedAt = now;
    }

    public void disable(OffsetDateTime now) {
        if (disabledAt == null) {
            this.disabledAt = now;
            this.updatedAt = now;
        }
    }
}
