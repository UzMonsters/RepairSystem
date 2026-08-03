package com.example.darks.repair_auto.review.api.dto;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.review.domain.RepairReview;

public final class ReviewMapper {

    private ReviewMapper() {
    }

    public static ReviewResponse response(RepairReview review) {
        RepairCategory category = review.getRepairRequest().getCategory();
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
                        category.getNameEn(),
                        category.getNameRu(),
                        category.getNameUz()));
    }
}
