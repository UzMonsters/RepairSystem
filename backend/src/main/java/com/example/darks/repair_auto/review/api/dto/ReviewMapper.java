package com.example.darks.repair_auto.review.api.dto;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.review.domain.RepairReview;

import com.example.darks.repair_auto.localization.application.LocalizedValueResolver;
import com.example.darks.repair_auto.settings.domain.Language;

public final class ReviewMapper {

    private ReviewMapper() {
    }

    public static ReviewResponse response(RepairReview review) {
        return response(review, null, null);
    }

    public static ReviewResponse response(RepairReview review, Language language, LocalizedValueResolver resolver) {
        RepairCategory category = review.getRepairRequest().getCategory();
        String name = resolver != null
                ? resolver.resolve(language, category.getNameUz(), category.getNameRu(), category.getNameEn())
                : (category.getNameUz() != null ? category.getNameUz() : (category.getNameRu() != null ? category.getNameRu() : category.getNameEn()));
        return new ReviewResponse(
                review.getId(),
                review.getRepairRequest().getId(),
                review.getRepairRequest().getRequestNumber(),
                review.getRating(),
                review.getComment(),
                review.getSource(),
                review.getSubmittedLanguage(),
                review.getSubmittedAt(),
                review.getCustomer().getId(),
                review.getCustomer().getFullName(),
                review.getTechnician().getId(),
                review.getTechnician().getFullName(),
                new ReviewCategoryResponse(
                        category.getId(),
                        name,
                        category.getNameEn(),
                        category.getNameRu(),
                        category.getNameUz()));
    }
}
