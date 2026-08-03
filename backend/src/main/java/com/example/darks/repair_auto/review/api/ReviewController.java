package com.example.darks.repair_auto.review.api;

import com.example.darks.repair_auto.review.api.dto.ReviewResponse;
import com.example.darks.repair_auto.review.api.dto.ReviewSummaryResponse;
import com.example.darks.repair_auto.review.application.RepairReviewService;
import com.example.darks.repair_auto.review.application.ReviewQuery;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final RepairReviewService reviewService;

    public ReviewController(RepairReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/v1/reviews")
    @Operation(summary = "List customer repair reviews")
    public PageResponse<ReviewResponse> list(
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long repairRequestId,
            @RequestParam(required = false) String requestNumber,
            @RequestParam(required = false) Boolean hasComment,
            @RequestParam(required = false) OffsetDateTime submittedFrom,
            @RequestParam(required = false) OffsetDateTime submittedTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {
        return reviewService.list(
                new ReviewQuery(
                        rating,
                        technicianId,
                        customerId,
                        categoryId,
                        repairRequestId,
                        requestNumber,
                        hasComment,
                        submittedFrom,
                        submittedTo),
                ReviewPageRequest.toPageable(page, size, sort));
    }

    @GetMapping("/api/v1/reviews/{reviewId}")
    @Operation(summary = "Get customer repair review details")
    public ReviewResponse get(@PathVariable Long reviewId) {
        return reviewService.get(reviewId);
    }

    @GetMapping("/api/v1/reviews/summary")
    @Operation(summary = "Summarize customer repair reviews")
    public ReviewSummaryResponse summary(
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long repairRequestId,
            @RequestParam(required = false) String requestNumber,
            @RequestParam(required = false) Boolean hasComment,
            @RequestParam(required = false) OffsetDateTime submittedFrom,
            @RequestParam(required = false) OffsetDateTime submittedTo) {
        return reviewService.summary(new ReviewQuery(
                rating,
                technicianId,
                customerId,
                categoryId,
                repairRequestId,
                requestNumber,
                hasComment,
                submittedFrom,
                submittedTo));
    }
}
