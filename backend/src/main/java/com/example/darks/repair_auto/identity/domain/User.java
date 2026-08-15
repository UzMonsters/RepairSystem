package com.example.darks.repair_auto.identity.domain;

import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(length = 30)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_attachment_id")
    private RepairAttachment avatarAttachment;

    @Column(name = "password_hash", nullable = false)
    @JsonIgnore
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "password_changed_at", nullable = false)
    private OffsetDateTime passwordChangedAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "auth_version", nullable = false)
    @JsonIgnore
    private long authVersion = 1L;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected User() {
    }

    public User(String fullName, String email, String passwordHash, UserRole role, boolean active, OffsetDateTime now) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.passwordChangedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName, OffsetDateTime now) {
        this.fullName = fullName;
        this.updatedAt = now;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email, OffsetDateTime now) {
        this.email = email;
        this.updatedAt = now;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void changePassword(String passwordHash, OffsetDateTime now) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = now;
        this.updatedAt = now;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role, OffsetDateTime now) {
        this.role = role;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active, OffsetDateTime now) {
        this.active = active;
        this.updatedAt = now;
    }

    public OffsetDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public long getAuthVersion() {
        return authVersion;
    }

    public void markLoggedIn(OffsetDateTime now) {
        this.lastLoginAt = now;
        this.updatedAt = now;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone, OffsetDateTime now) {
        this.phone = phone;
        this.updatedAt = now;
    }

    public RepairAttachment getAvatarAttachment() {
        return avatarAttachment;
    }

    public void setAvatarAttachment(RepairAttachment avatarAttachment, OffsetDateTime now) {
        this.avatarAttachment = avatarAttachment;
        this.updatedAt = now;
    }
}
