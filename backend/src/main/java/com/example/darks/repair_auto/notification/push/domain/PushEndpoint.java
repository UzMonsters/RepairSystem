package com.example.darks.repair_auto.notification.push.domain;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.identity.domain.User;
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
import java.util.Objects;

@Entity
@Table(name = "push_endpoints")
public class PushEndpoint {

    public static final int MAX_FCM_REGISTRATION_TOKEN_LENGTH = 512;
    public static final int MAX_APP_VERSION_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 32)
    private PushOwnerType ownerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_user_id")
    private User staffUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 64)
    private PushClientType clientType;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 32)
    private PushPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "firebase_app_key", nullable = false, length = 64)
    private PushFirebaseApp firebaseAppKey;

    @Column(name = "fcm_registration_token", nullable = false, length = 512)
    private String fcmRegistrationToken;

    @Column(name = "app_version", length = 64)
    private String appVersion;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected PushEndpoint() {
    }

    private PushEndpoint(
            PushOwnerType ownerType,
            User staffUser,
            Customer customer,
            Technician technician,
            PushClientType clientType,
            PushPlatform platform,
            PushFirebaseApp firebaseAppKey,
            String fcmRegistrationToken,
            String appVersion,
            boolean enabled,
            OffsetDateTime lastSeenAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this.ownerType = Objects.requireNonNull(ownerType, "ownerType must not be null");
        this.staffUser = staffUser;
        this.customer = customer;
        this.technician = technician;
        this.clientType = Objects.requireNonNull(clientType, "clientType must not be null");
        this.platform = Objects.requireNonNull(platform, "platform must not be null");
        this.firebaseAppKey = Objects.requireNonNull(firebaseAppKey, "firebaseAppKey must not be null");
        this.fcmRegistrationToken = Objects.requireNonNull(fcmRegistrationToken, "fcmRegistrationToken must not be null");
        this.appVersion = appVersion;
        this.enabled = enabled;
        this.lastSeenAt = Objects.requireNonNull(lastSeenAt, "lastSeenAt must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static PushEndpoint forStaff(
            User staffUser,
            PushClientType clientType,
            PushPlatform platform,
            PushFirebaseApp firebaseAppKey,
            String fcmRegistrationToken,
            String appVersion,
            OffsetDateTime now) {
        Objects.requireNonNull(staffUser, "staffUser must not be null");
        return new PushEndpoint(
                PushOwnerType.STAFF,
                staffUser,
                null,
                null,
                clientType,
                platform,
                firebaseAppKey,
                fcmRegistrationToken,
                appVersion,
                true,
                now,
                now,
                now);
    }

    public static PushEndpoint forCustomer(
            Customer customer,
            PushClientType clientType,
            PushPlatform platform,
            PushFirebaseApp firebaseAppKey,
            String fcmRegistrationToken,
            String appVersion,
            OffsetDateTime now) {
        Objects.requireNonNull(customer, "customer must not be null");
        return new PushEndpoint(
                PushOwnerType.CUSTOMER,
                null,
                customer,
                null,
                clientType,
                platform,
                firebaseAppKey,
                fcmRegistrationToken,
                appVersion,
                true,
                now,
                now,
                now);
    }

    public static PushEndpoint forTechnician(
            Technician technician,
            PushClientType clientType,
            PushPlatform platform,
            PushFirebaseApp firebaseAppKey,
            String fcmRegistrationToken,
            String appVersion,
            OffsetDateTime now) {
        Objects.requireNonNull(technician, "technician must not be null");
        return new PushEndpoint(
                PushOwnerType.TECHNICIAN,
                null,
                null,
                technician,
                clientType,
                platform,
                firebaseAppKey,
                fcmRegistrationToken,
                appVersion,
                true,
                now,
                now,
                now);
    }

    public void reassignToStaff(
            User staffUser,
            PushClientType clientType,
            PushPlatform platform,
            PushFirebaseApp firebaseAppKey,
            String appVersion,
            OffsetDateTime now) {
        this.ownerType = PushOwnerType.STAFF;
        this.staffUser = Objects.requireNonNull(staffUser, "staffUser must not be null");
        this.customer = null;
        this.technician = null;
        this.clientType = Objects.requireNonNull(clientType, "clientType must not be null");
        this.platform = Objects.requireNonNull(platform, "platform must not be null");
        this.firebaseAppKey = Objects.requireNonNull(firebaseAppKey, "firebaseAppKey must not be null");
        if (appVersion != null && !appVersion.isBlank()) {
            this.appVersion = appVersion;
        }
        this.enabled = true;
        this.lastSeenAt = Objects.requireNonNull(now, "now must not be null");
        this.updatedAt = now;
    }

    public void reassignToCustomer(
            Customer customer,
            PushClientType clientType,
            PushPlatform platform,
            PushFirebaseApp firebaseAppKey,
            String appVersion,
            OffsetDateTime now) {
        this.ownerType = PushOwnerType.CUSTOMER;
        this.staffUser = null;
        this.customer = Objects.requireNonNull(customer, "customer must not be null");
        this.technician = null;
        this.clientType = Objects.requireNonNull(clientType, "clientType must not be null");
        this.platform = Objects.requireNonNull(platform, "platform must not be null");
        this.firebaseAppKey = Objects.requireNonNull(firebaseAppKey, "firebaseAppKey must not be null");
        if (appVersion != null && !appVersion.isBlank()) {
            this.appVersion = appVersion;
        }
        this.enabled = true;
        this.lastSeenAt = Objects.requireNonNull(now, "now must not be null");
        this.updatedAt = now;
    }

    public void reassignToTechnician(
            Technician technician,
            PushClientType clientType,
            PushPlatform platform,
            PushFirebaseApp firebaseAppKey,
            String appVersion,
            OffsetDateTime now) {
        this.ownerType = PushOwnerType.TECHNICIAN;
        this.staffUser = null;
        this.customer = null;
        this.technician = Objects.requireNonNull(technician, "technician must not be null");
        this.clientType = Objects.requireNonNull(clientType, "clientType must not be null");
        this.platform = Objects.requireNonNull(platform, "platform must not be null");
        this.firebaseAppKey = Objects.requireNonNull(firebaseAppKey, "firebaseAppKey must not be null");
        if (appVersion != null && !appVersion.isBlank()) {
            this.appVersion = appVersion;
        }
        this.enabled = true;
        this.lastSeenAt = Objects.requireNonNull(now, "now must not be null");
        this.updatedAt = now;
    }

    public void touch(String appVersion, OffsetDateTime now) {
        this.enabled = true;
        this.lastSeenAt = Objects.requireNonNull(now, "now must not be null");
        if (appVersion != null && !appVersion.isBlank()) {
            this.appVersion = appVersion;
        }
        this.updatedAt = now;
    }

    public void disable(OffsetDateTime now) {
        this.enabled = false;
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public boolean isOwnedByStaff(Long userId) {
        return ownerType == PushOwnerType.STAFF && staffUser != null && Objects.equals(staffUser.getId(), userId);
    }

    public boolean isOwnedByCustomer(Long customerId) {
        return ownerType == PushOwnerType.CUSTOMER && customer != null && Objects.equals(customer.getId(), customerId);
    }

    public boolean isOwnedByTechnician(Long technicianId) {
        return ownerType == PushOwnerType.TECHNICIAN && technician != null && Objects.equals(technician.getId(), technicianId);
    }

    public Long getId() {
        return id;
    }

    public PushOwnerType getOwnerType() {
        return ownerType;
    }

    public User getStaffUser() {
        return staffUser;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Technician getTechnician() {
        return technician;
    }

    public PushClientType getClientType() {
        return clientType;
    }

    public PushPlatform getPlatform() {
        return platform;
    }

    public PushFirebaseApp getFirebaseAppKey() {
        return firebaseAppKey;
    }

    public String getFcmRegistrationToken() {
        return fcmRegistrationToken;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public OffsetDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
