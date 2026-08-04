package com.example.darks.repair_auto.dashboard.api.dto;

import java.math.BigDecimal;

public record RequestCategoryDistributionItemResponse(
        Long categoryId,
        String nameEn,
        String nameRu,
        String nameUz,
        long count,
        BigDecimal percentage) {
}
