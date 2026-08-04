package com.example.darks.repair_auto.review.api;

import com.example.darks.repair_auto.shared.pagination.PageableRequestFactory;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;

public final class ReviewPageRequest {

    private static final String DEFAULT_SORT = "submittedAt,desc";
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "submittedAt", "submittedAt",
            "rating", "rating",
            "requestNumber", "repairRequest.requestNumber");

    private ReviewPageRequest() {
    }

    public static Pageable toPageable(Integer page, Integer size, List<String> sort) {
        return PageableRequestFactory.toPageable(page, size, sort, DEFAULT_SORT, SORT_PROPERTIES);
    }
}
