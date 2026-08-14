package com.example.darks.repair_auto.catalog.category.api.dto;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.localization.application.LocalizedValueResolver;
import com.example.darks.repair_auto.settings.domain.Language;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategorySummaryResponse summary(RepairCategory category, Language language, LocalizedValueResolver resolver) {
        String name = resolver.resolve(language, category.getNameUz(), category.getNameRu(), category.getNameEn());
        String description = resolver.resolve(language, category.getDescriptionUz(), category.getDescriptionRu(), category.getDescriptionEn());
        return new CategorySummaryResponse(
                category.getId(),
                name,
                description,
                category.getNameEn(),
                category.getNameRu(),
                category.getNameUz(),
                category.getDescriptionEn(),
                category.getDescriptionRu(),
                category.getDescriptionUz(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    public static CategoryDetailResponse details(RepairCategory category, Language language, LocalizedValueResolver resolver) {
        String name = resolver.resolve(language, category.getNameUz(), category.getNameRu(), category.getNameEn());
        String description = resolver.resolve(language, category.getDescriptionUz(), category.getDescriptionRu(), category.getDescriptionEn());
        return new CategoryDetailResponse(
                category.getId(),
                name,
                description,
                category.getNameEn(),
                category.getNameRu(),
                category.getNameUz(),
                category.getDescriptionEn(),
                category.getDescriptionRu(),
                category.getDescriptionUz(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
