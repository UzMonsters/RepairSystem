package com.example.darks.repair_auto.catalog.category.api.dto;

import java.time.OffsetDateTime;

public record CategoryDetailResponse(
        Long id,
        String name,
        String description,
        String nameEn,
        String nameRu,
        String nameUz,
        String descriptionEn,
        String descriptionRu,
        String descriptionUz,
        boolean active,
        int displayOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
