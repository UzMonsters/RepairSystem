package com.example.darks.repair_auto.review.application;

import java.time.OffsetDateTime;

public record ReviewQuery(
        Integer rating,
        Long technicianId,
        Long customerId,
        Long categoryId,
        Long repairRequestId,
        String requestNumber,
        Boolean hasComment,
        OffsetDateTime submittedFrom,
        OffsetDateTime submittedTo) {
}
