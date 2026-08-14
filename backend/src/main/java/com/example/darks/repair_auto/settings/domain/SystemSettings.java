package com.example.darks.repair_auto.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "system_settings")
public class SystemSettings {

    @Id
    private Long id = 1L;

    @Column(nullable = false, length = 64)
    private String timezone = "Asia/Tashkent";

    @Enumerated(EnumType.STRING)
    @Column(name = "default_language", nullable = false, length = 10)
    private Language defaultLanguage = Language.UZ;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    protected SystemSettings() {
    }

    public SystemSettings(Long id, String timezone, Language defaultLanguage, OffsetDateTime now, Long updatedBy) {
        this.id = id != null ? id : 1L;
        this.timezone = timezone != null ? timezone : "Asia/Tashkent";
        this.defaultLanguage = defaultLanguage != null ? defaultLanguage : Language.UZ;
        this.updatedAt = now;
        this.updatedBy = updatedBy;
    }

    public Long getId() {
        return id;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone, OffsetDateTime now, Long updatedBy) {
        this.timezone = timezone != null ? timezone : "Asia/Tashkent";
        this.updatedAt = now;
        this.updatedBy = updatedBy;
    }

    public Language getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(Language defaultLanguage, OffsetDateTime now, Long updatedBy) {
        this.defaultLanguage = defaultLanguage != null ? defaultLanguage : Language.UZ;
        this.updatedAt = now;
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }
}
