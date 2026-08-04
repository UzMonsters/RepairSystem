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
        this.fullName = fullName;
        this.phone = phone;
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
        this.fullName = fullName;
        this.phone = phone;
        this.specialization = specialization;
        this.notes = notes;
        this.maximumConcurrentRequests = maximumConcurrentRequests;
        this.preferredLanguage = preferredLanguage;
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

    public void unlinkTelegram(OffsetDateTime now) {
        this.telegramUserId = null;
        this.telegramChatId = null;
        this.telegramLinkedAt = null;
        this.updatedAt = now;
    }
}
