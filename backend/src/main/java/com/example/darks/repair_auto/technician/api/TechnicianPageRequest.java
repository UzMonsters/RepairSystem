package com.example.darks.repair_auto.technician.api;

import com.example.darks.repair_auto.shared.pagination.PageableRequestFactory;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;

public final class TechnicianPageRequest {

    public static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "id",
            "fullName",
            "phone",
            "specialization",
            "maximumConcurrentRequests",
            "active",
            "createdAt",
            "updatedAt");

    private static final String DEFAULT_SORT = "createdAt,desc";
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "id", "id",
            "fullName", "fullName",
            "phone", "phone",
            "specialization", "specialization",
            "maximumConcurrentRequests", "maximumConcurrentRequests",
            "active", "active",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt");

    private TechnicianPageRequest() {
    }

    public static Pageable toPageable(Integer page, Integer size, List<String> sort) {
        return PageableRequestFactory.toPageable(page, size, sort, DEFAULT_SORT, SORT_PROPERTIES);
    }
}
