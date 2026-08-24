package com.example.darks.repair_auto.identity.domain;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
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
@Table(name = "mobile_sessions")
public class MobileSession {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 32)
    private PushClientType clientType;

    @Enumerated(EnumType.STRING)
    @Column(name = "authentication_provider", nullable = false, length = 32)
    private MobileAuthProvider authenticationProvider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PushPlatform platform;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column(name = "device_name", length = 160)
    private String deviceName;

    @Column(name = "app_version", length = 64)
    private String appVersion;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revocation_reason", length = 64)
    private String revocationReason;

    @Column(name = "created_ip", length = 64)
    private String createdIp;

    @Column(name = "last_ip", length = 64)
    private String lastIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected MobileSession() {
    }

    private MobileSession(
            ActorType actorType,
            Customer customer,
            Technician technician,
            PushClientType clientType,
            MobileAuthProvider provider,
            PushPlatform platform,
            String deviceId,
            String deviceName,
            String appVersion,
            String ip,
            String userAgent,
            OffsetDateTime now,
            OffsetDateTime expiresAt) {
        this.id = UUID.randomUUID();
        this.actorType = actorType;
        this.customer = customer;
        this.technician = technician;
        this.clientType = clientType;
        this.authenticationProvider = provider;
        this.platform = platform;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.appVersion = appVersion;
        this.createdIp = ip;
        this.lastIp = ip;
        this.userAgent = userAgent;
        this.createdAt = now;
        this.lastSeenAt = now;
        this.expiresAt = expiresAt;
        this.updatedAt = now;
    }

    public static MobileSession forCustomer(
            Customer customer,
            MobileAuthProvider provider,
            PushPlatform platform,
            String deviceId,
            String deviceName,
            String appVersion,
            String ip,
            String userAgent,
            OffsetDateTime now,
            OffsetDateTime expiresAt) {
        return new MobileSession(
                ActorType.CUSTOMER,
                customer,
                null,
                PushClientType.CUSTOMER_MOBILE,
                provider,
                platform,
                deviceId,
                deviceName,
                appVersion,
                ip,
                userAgent,
                now,
                expiresAt);
    }

    public static MobileSession forTechnician(
            Technician technician,
            MobileAuthProvider provider,
            PushPlatform platform,
            String deviceId,
            String deviceName,
            String appVersion,
            String ip,
            String userAgent,
            OffsetDateTime now,
            OffsetDateTime expiresAt) {
        return new MobileSession(
                ActorType.TECHNICIAN,
                null,
                technician,
                PushClientType.TECHNICIAN_MOBILE,
                provider,
                platform,
                deviceId,
                deviceName,
                appVersion,
                ip,
                userAgent,
                now,
                expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public Long getActorId() {
        return actorType == ActorType.CUSTOMER ? customer.getId() : technician.getId();
    }

    public PushClientType getClientType() {
        return clientType;
    }

    public MobileAuthProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public PushPlatform getPlatform() {
        return platform;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public boolean isActiveAt(OffsetDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void touch(String ip, OffsetDateTime now) {
        this.lastIp = ip;
        this.lastSeenAt = now;
        this.updatedAt = now;
    }

    public void revoke(OffsetDateTime now, MobileSessionRevocationReason reason) {
        if (this.revokedAt == null) {
            this.revokedAt = now;
            this.revocationReason = reason.name();
            this.updatedAt = now;
        }
    }
}
