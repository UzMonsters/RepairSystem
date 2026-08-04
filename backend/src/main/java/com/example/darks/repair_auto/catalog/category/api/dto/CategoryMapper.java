package com.example.darks.repair_auto.catalog.category.api.dto;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategorySummaryResponse summary(RepairCategory category) {
        return new CategorySummaryResponse(
                category.getId(),
                category.getNameEn(),
                category.getNameRu(),
                category.getNameUz(),
                category.isActive(),
                category.getDisplayOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    public static CategoryDetailResponse details(RepairCategory category) {
        return new CategoryDetailResponse(
                category.getId(),
                category.getNameEn(),
                category.getNameRu(),
                category.getNameUz(),
                category.getDescriptionEn(),
                category.getDescriptionRu(),
                category.getDescriptionUz(),
                category.isActive(),
                category.getDisplayOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
