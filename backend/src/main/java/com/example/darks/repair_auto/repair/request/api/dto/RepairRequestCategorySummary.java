package com.example.darks.repair_auto.repair.request.api.dto;

public record RepairRequestCategorySummary(
        Long id,
        String name,
        String description,
        String nameEn,
        String nameRu,
        String nameUz,
        boolean active) {
}
