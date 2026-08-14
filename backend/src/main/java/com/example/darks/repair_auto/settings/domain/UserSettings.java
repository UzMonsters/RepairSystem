package com.example.darks.repair_auto.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Language language = Language.UZ;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_format", nullable = false, length = 30)
    private DateFormat dateFormat = DateFormat.DD_MM_YYYY;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_format", nullable = false, length = 20)
    private TimeFormat timeFormat = TimeFormat.HOUR_24;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Theme theme = Theme.SYSTEM;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected UserSettings() {
    }

    public UserSettings(Long userId, Language language, DateFormat dateFormat, TimeFormat timeFormat, Theme theme, OffsetDateTime now) {
        this.userId = userId;
        this.language = language != null ? language : Language.UZ;
        this.dateFormat = dateFormat != null ? dateFormat : DateFormat.DD_MM_YYYY;
        this.timeFormat = timeFormat != null ? timeFormat : TimeFormat.HOUR_24;
        this.theme = theme != null ? theme : Theme.SYSTEM;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language, OffsetDateTime now) {
        this.language = language != null ? language : Language.UZ;
        this.updatedAt = now;
    }

    public DateFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(DateFormat dateFormat, OffsetDateTime now) {
        this.dateFormat = dateFormat != null ? dateFormat : DateFormat.DD_MM_YYYY;
        this.updatedAt = now;
    }

    public TimeFormat getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(TimeFormat timeFormat, OffsetDateTime now) {
        this.timeFormat = timeFormat != null ? timeFormat : TimeFormat.HOUR_24;
        this.updatedAt = now;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme, OffsetDateTime now) {
        this.theme = theme != null ? theme : Theme.SYSTEM;
        this.updatedAt = now;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
