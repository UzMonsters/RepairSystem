package com.example.darks.repair_auto.catalog.category.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;

@Entity
@Table(name = "repair_categories")
public class RepairCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_en", nullable = false, length = 120)
    private String nameEn;

    @Column(name = "name_ru", nullable = false, length = 120)
    private String nameRu;

    @Column(name = "name_uz", nullable = false, length = 120)
    private String nameUz;

    @JsonIgnore
    @Column(name = "name_en_normalized", nullable = false, unique = true, length = 120)
    private String nameEnNormalized;

    @JsonIgnore
    @Column(name = "name_uz_normalized", nullable = false, unique = true, length = 120)
    private String nameUzNormalized;

    @JsonIgnore
    @Column(name = "name_ru_normalized", nullable = false, unique = true, length = 120)
    private String nameRuNormalized;

    @Column(name = "description_en", length = 500)
    private String descriptionEn;

    @Column(name = "description_ru", length = 500)
    private String descriptionRu;

    @Column(name = "description_uz", length = 500)
    private String descriptionUz;

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

    protected RepairCategory() {
    }

    public RepairCategory(
            String nameEn,
            String nameRu,
            String nameUz,
            String nameEnNormalized,
            String nameRuNormalized,
            String nameUzNormalized,
            String descriptionEn,
            String descriptionRu,
            String descriptionUz,
            Boolean active,
            OffsetDateTime now) {
        this.nameEn = nameEn;
        this.nameEnNormalized = nameEnNormalized;
        this.nameUz = nameUz;
        this.nameRu = nameRu;
        this.nameUzNormalized = nameUzNormalized;
        this.nameRuNormalized = nameRuNormalized;
        this.descriptionEn = descriptionEn;
        this.descriptionUz = descriptionUz;
        this.descriptionRu = descriptionRu;
        this.active = active == null || active;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getNameUz() {
        return nameUz;
    }

    public String getNameRu() {
        return nameRu;
    }

    public String getNameEnNormalized() {
        return nameEnNormalized;
    }

    public String getNameUzNormalized() {
        return nameUzNormalized;
    }

    public String getNameRuNormalized() {
        return nameRuNormalized;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public String getDescriptionUz() {
        return descriptionUz;
    }

    public String getDescriptionRu() {
        return descriptionRu;
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

    public void update(
            String nameEn,
            String nameRu,
            String nameUz,
            String nameEnNormalized,
            String nameRuNormalized,
            String nameUzNormalized,
            String descriptionEn,
            String descriptionRu,
            String descriptionUz,
            OffsetDateTime now) {
        this.nameEn = nameEn;
        this.nameUz = nameUz;
        this.nameRu = nameRu;
        this.nameEnNormalized = nameEnNormalized;
        this.nameUzNormalized = nameUzNormalized;
        this.nameRuNormalized = nameRuNormalized;
        this.descriptionEn = descriptionEn;
        this.descriptionUz = descriptionUz;
        this.descriptionRu = descriptionRu;
        this.updatedAt = now;
    }

    public void setActive(boolean active, OffsetDateTime now) {
        this.active = active;
        this.updatedAt = now;
    }
}
