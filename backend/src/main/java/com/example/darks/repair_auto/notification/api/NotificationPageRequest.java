package com.example.darks.repair_auto.notification.api;

import com.example.darks.repair_auto.shared.pagination.PageableRequestFactory;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;

public final class NotificationPageRequest {

    private static final String DEFAULT_SORT = "createdAt,desc";
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "id", "id",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "status", "status",
            "notificationType", "notificationType",
            "nextAttemptAt", "nextAttemptAt");

    private NotificationPageRequest() {
    }

    public static Pageable toPageable(Integer page, Integer size, List<String> sort) {
        return PageableRequestFactory.toPageable(page, size, sort, DEFAULT_SORT, SORT_PROPERTIES);
    }
}
