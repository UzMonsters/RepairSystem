package com.example.darks.repair_auto.catalog.category.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank @Size(max = 120) String nameEn,
        @NotBlank @Size(max = 120) String nameRu,
        @NotBlank @Size(max = 120) String nameUz,
        @Size(max = 500) String descriptionEn,
        @Size(max = 500) String descriptionRu,
        @Size(max = 500) String descriptionUz,
        Boolean active) {
}
