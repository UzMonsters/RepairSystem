package com.example.darks.repair_auto.catalog.category.api;

import com.example.darks.repair_auto.shared.pagination.PageableRequestFactory;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;

public final class CategoryPageRequest {

    public static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "id",
            "nameEn",
            "nameUz",
            "nameRu",
            "active",
            "displayOrder",
            "createdAt",
            "updatedAt");

    private static final String DEFAULT_SORT = "displayOrder,asc";
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "id", "id",
            "nameEn", "nameEn",
            "nameUz", "nameUz",
            "nameRu", "nameRu",
            "active", "active",
            "displayOrder", "displayOrder",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt");

    private CategoryPageRequest() {
    }

    public static Pageable toPageable(Integer page, Integer size, List<String> sort) {
        return PageableRequestFactory.toPageable(page, size, sort, DEFAULT_SORT, SORT_PROPERTIES);
    }
}
