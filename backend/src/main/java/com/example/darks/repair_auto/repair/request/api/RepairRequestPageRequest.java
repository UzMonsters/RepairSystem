package com.example.darks.repair_auto.repair.request.api;

import com.example.darks.repair_auto.shared.pagination.PageableRequestFactory;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;

public final class RepairRequestPageRequest {

    public static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "id",
            "requestNumber",
            "priority",
            "status",
            "source",
            "customerPreferredVisitAt",
            "createdAt",
            "updatedAt",
            "customerName",
            "categoryName");

    private static final String DEFAULT_SORT = "createdAt,desc";
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "id", "id",
            "requestNumber", "requestNumber",
            "priority", "priority",
            "status", "status",
            "source", "source",
            "customerPreferredVisitAt", "customerPreferredVisitAt",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "customerName", "customer.fullName",
            "categoryName", "category.nameEn");

    private RepairRequestPageRequest() {
    }

    public static Pageable toPageable(Integer page, Integer size, List<String> sort) {
        return PageableRequestFactory.toPageable(page, size, sort, DEFAULT_SORT, SORT_PROPERTIES);
    }
}
