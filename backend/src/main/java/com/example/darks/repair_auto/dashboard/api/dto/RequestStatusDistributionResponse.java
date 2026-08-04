package com.example.darks.repair_auto.dashboard.api.dto;

import java.util.List;

public record RequestStatusDistributionResponse(
        long total,
        List<RequestStatusDistributionItemResponse> items) {
}
