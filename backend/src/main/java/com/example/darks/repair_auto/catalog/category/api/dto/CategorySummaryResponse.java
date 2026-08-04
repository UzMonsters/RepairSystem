package com.example.darks.repair_auto.catalog.category.api.dto;

import java.time.OffsetDateTime;

public record CategorySummaryResponse(
        Long id,
        String nameEn,
        String nameRu,
        String nameUz,
        boolean active,
        int displayOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
