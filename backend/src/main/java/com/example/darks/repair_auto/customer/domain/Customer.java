package com.example.darks.repair_auto.customer.domain;

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
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, unique = true, length = 13)
    private String phone;

    @JsonIgnore
    @Column(name = "telegram_user_id", unique = true)
    private Long telegramUserId;

    @JsonIgnore
    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", nullable = false, length = 8)
    private LanguageCode preferredLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_source", nullable = false, length = 32)
    private CustomerRegistrationSource registrationSource;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JsonIgnore
    @Version
    @Column(nullable = false)
    private long version;

    protected Customer() {
    }

    public Customer(String fullName, String phone, LanguageCode preferredLanguage, OffsetDateTime now) {
        this.fullName = fullName;
        this.phone = phone;
        this.preferredLanguage = preferredLanguage == null ? LanguageCode.UZ : preferredLanguage;
        this.registrationSource = CustomerRegistrationSource.ADMIN;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Customer telegram(
            String fullName,
            String phone,
            Long telegramUserId,
            Long telegramChatId,
            LanguageCode preferredLanguage,
            OffsetDateTime now) {
        Customer customer = new Customer(fullName, phone, preferredLanguage, now);
        customer.registrationSource = CustomerRegistrationSource.TELEGRAM;
        customer.telegramUserId = telegramUserId;
        customer.telegramChatId = telegramChatId;
        return customer;
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

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public LanguageCode getPreferredLanguage() {
        return preferredLanguage;
    }

    public CustomerRegistrationSource getRegistrationSource() {
        return registrationSource;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isTelegramLinked() {
        return telegramUserId != null;
    }

    public void updateProfile(String fullName, String phone, LanguageCode preferredLanguage, OffsetDateTime now) {
        this.fullName = fullName;
        this.phone = phone;
        this.preferredLanguage = preferredLanguage;
        this.updatedAt = now;
    }

    public void linkTelegram(Long userId, Long chatId, LanguageCode language, OffsetDateTime now) {
        this.telegramUserId = userId;
        this.telegramChatId = chatId;
        this.preferredLanguage = language;
        this.updatedAt = now;
    }

    public void updateTelegramChat(Long chatId, LanguageCode language, OffsetDateTime now) {
        this.telegramChatId = chatId;
        this.preferredLanguage = language;
        this.updatedAt = now;
    }

    public void setActive(boolean active, OffsetDateTime now) {
        this.active = active;
        this.updatedAt = now;
    }
}
