package com.example.darks.repair_auto.catalog.category.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CategoryReorderRequest(@NotEmpty List<@Valid Item> items) {

    public record Item(
            @NotNull Long categoryId,
            @Min(0) int displayOrder) {
    }
}
