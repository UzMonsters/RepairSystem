package com.example.darks.repair_auto.technician.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;

@Entity
@Table(name = "technicians")
public class Technician {

    public static final int DEFAULT_MAXIMUM_CONCURRENT_REQUESTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, unique = true, length = 13)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    @Column(name = "phone_verified_at")
    private OffsetDateTime phoneVerifiedAt;

    @Column(length = 120)
    private String specialization;

    @Column(length = 1000)
    private String notes;

    @Column(name = "maximum_concurrent_requests", nullable = false)
    private int maximumConcurrentRequests;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", nullable = false, length = 8)
    private LanguageCode preferredLanguage;

    @Column(nullable = false)
    private boolean active;

    @JsonIgnore
    @Column(name = "auth_version", nullable = false)
    private long authVersion;

    @JsonIgnore
    @Column(name = "telegram_user_id", unique = true)
    private Long telegramUserId;

    @JsonIgnore
    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    @JsonIgnore
    @Column(name = "telegram_linked_at")
    private OffsetDateTime telegramLinkedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JsonIgnore
    @Version
    @Column(nullable = false)
    private long version;

    protected Technician() {
    }

    public Technician(
            String fullName,
            String phone,
            String specialization,
            String notes,
            Integer maximumConcurrentRequests,
            LanguageCode preferredLanguage,
            Boolean active,
            OffsetDateTime now) {
        this(fullName, phone, null, specialization, notes, maximumConcurrentRequests, preferredLanguage, active, now);
    }

    public Technician(
            String fullName,
            String phone,
            String email,
            String specialization,
            String notes,
            Integer maximumConcurrentRequests,
            LanguageCode preferredLanguage,
            Boolean active,
            OffsetDateTime now) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.specialization = specialization;
        this.notes = notes;
        this.maximumConcurrentRequests = maximumConcurrentRequests == null
                ? DEFAULT_MAXIMUM_CONCURRENT_REQUESTS : maximumConcurrentRequests;
        this.preferredLanguage = preferredLanguage == null ? LanguageCode.UZ : preferredLanguage;
        this.active = active == null || active;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public OffsetDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public OffsetDateTime getPhoneVerifiedAt() {
        return phoneVerifiedAt;
    }

    public String getSpecialization() {
        return specialization;
    }

    public String getNotes() {
        return notes;
    }

    public int getMaximumConcurrentRequests() {
        return maximumConcurrentRequests;
    }

    public LanguageCode getPreferredLanguage() {
        return preferredLanguage;
    }

    public boolean isActive() {
        return active;
    }

    public long getAuthVersion() {
        return authVersion;
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public OffsetDateTime getTelegramLinkedAt() {
        return telegramLinkedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isTelegramLinked() {
        return telegramUserId != null || telegramLinkedAt != null;
    }

    public void updateProfile(
            String fullName,
            String phone,
            String specialization,
            String notes,
            int maximumConcurrentRequests,
            LanguageCode preferredLanguage,
            OffsetDateTime now) {
        updateProfile(fullName, phone, this.email, specialization, notes, maximumConcurrentRequests, preferredLanguage, now);
    }

    public void updateProfile(
            String fullName,
            String phone,
            String email,
            String specialization,
            String notes,
            int maximumConcurrentRequests,
            LanguageCode preferredLanguage,
            OffsetDateTime now) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.specialization = specialization;
        this.notes = notes;
        this.maximumConcurrentRequests = maximumConcurrentRequests;
        this.preferredLanguage = preferredLanguage;
        this.updatedAt = now;
    }

    public void setEmail(String email, OffsetDateTime verifiedAt, OffsetDateTime now) {
        this.email = email;
        this.emailVerifiedAt = verifiedAt;
        this.updatedAt = now;
    }

    public void removeEmail(OffsetDateTime now) {
        this.email = null;
        this.emailVerifiedAt = null;
        this.updatedAt = now;
    }

    public void setPhone(String phone, OffsetDateTime phoneVerifiedAt, OffsetDateTime now) {
        this.phone = phone;
        this.phoneVerifiedAt = phoneVerifiedAt;
        this.updatedAt = now;
    }

    public void markPhoneVerified(OffsetDateTime now) {
        this.phoneVerifiedAt = now;
        this.updatedAt = now;
    }

    public void incrementAuthVersion(OffsetDateTime now) {
        this.authVersion++;
        this.updatedAt = now;
    }

    public void setActive(boolean active, OffsetDateTime now) {
        this.active = active;
        this.updatedAt = now;
    }

    public void linkTelegram(Long telegramUserId, Long telegramChatId, OffsetDateTime now) {
        this.telegramUserId = telegramUserId;
        this.telegramChatId = telegramChatId;
        this.telegramLinkedAt = now;
        this.updatedAt = now;
    }

    public void updateTelegramLanguage(LanguageCode preferredLanguage, OffsetDateTime now) {
        this.preferredLanguage = preferredLanguage == null ? LanguageCode.UZ : preferredLanguage;
        this.updatedAt = now;
    }

    public void unlinkTelegram(OffsetDateTime now) {
        this.telegramUserId = null;
        this.telegramChatId = null;
        this.telegramLinkedAt = null;
        this.updatedAt = now;
    }
}
