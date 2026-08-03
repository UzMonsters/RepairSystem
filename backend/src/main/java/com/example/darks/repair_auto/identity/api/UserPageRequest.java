package com.example.darks.repair_auto.identity.api;

import com.example.darks.repair_auto.shared.error.InvalidRequestParameterException;
import java.util.List;
import java.util.Map;
import com.example.darks.repair_auto.shared.pagination.PageableRequestFactory;
import org.springframework.data.domain.Pageable;

public final class UserPageRequest {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "id",
            "fullName",
            "email",
            "role",
            "active",
            "createdAt",
            "updatedAt",
            "lastLoginAt");

    private static final String DEFAULT_SORT = "createdAt,desc";
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "id", "id",
            "fullName", "fullName",
            "email", "email",
            "role", "role",
            "active", "active",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "lastLoginAt", "lastLoginAt");

    private UserPageRequest() {
    }

    public static Pageable toPageable(Integer page, Integer size, List<String> sort) {
        return PageableRequestFactory.toPageable(page, size, sort, DEFAULT_SORT, SORT_PROPERTIES);
    }

    private static InvalidRequestParameterException invalid(String field, String message) {
        return new InvalidRequestParameterException(field, message);
    }
}
