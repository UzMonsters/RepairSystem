package com.example.darks.repair_auto.repair.request.mobile.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerRepairRequestCreateRequest;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerRepairRequestDetailResponse;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerRepairRequestSummaryResponse;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerRepairRequestTimelineItemResponse;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerReviewCreateRequest;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerReviewResponse;
import com.example.darks.repair_auto.repair.request.mobile.application.CustomerRepairRequestFacade;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/me/repair-requests")
@Tag(name = "Customer Mobile Repair Requests", description = "Endpoints for Customer self-service repair request management")
@SecurityRequirement(name = "bearerAuth")
public class CustomerRepairRequestController {

    private final CustomerRepairRequestFacade facade;

    public CustomerRepairRequestController(CustomerRepairRequestFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    @Operation(
            summary = "Create Customer Repair Request",
            description = "Creates a new repair request owned by the authenticated Customer. "
                    + "Requires an Idempotency-Key header for retry safety.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Repair request created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid payload"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive"),
            @ApiResponse(responseCode = "409", description = "Idempotency conflict or concurrent submission conflict")
    })
    public ResponseEntity<CustomerRepairRequestDetailResponse> createRequest(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @Parameter(description = "Unique client-generated idempotency key (UUID or string up to 120 chars)", required = true)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CustomerRepairRequestCreateRequest request) {
        CustomerRepairRequestDetailResponse response = facade.createRequest(actor, idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List Own Repair Requests",
            description = "Returns paginated list of repair requests belonging to the authenticated Customer with optional status and category filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of customer repair requests returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive")
    })
    public PageResponse<CustomerRepairRequestSummaryResponse> listRequests(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @Parameter(description = "Optional filter by request status")
            @RequestParam(required = false) RepairRequestStatus status,
            @Parameter(description = "Optional filter by repair category ID")
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return facade.listRequests(actor, status, categoryId, pageable);
    }

    @GetMapping("/{requestId}")
    @Operation(
            summary = "Get Own Repair Request Detail",
            description = "Returns detailed information of a repair request owned by the authenticated Customer. "
                    + "Returns 404 if the request does not exist or belongs to another Customer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer repair request details returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive"),
            @ApiResponse(responseCode = "404", description = "Repair request not found")
    })
    public CustomerRepairRequestDetailResponse getRequestDetail(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId) {
        return facade.getRequestDetail(actor, requestId);
    }

    @GetMapping("/{requestId}/timeline")
    @Operation(
            summary = "Get Own Repair Request Status Timeline",
            description = "Returns status transition history in chronological order for a request owned by the authenticated Customer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chronological status timeline returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive"),
            @ApiResponse(responseCode = "404", description = "Repair request not found")
    })
    public List<CustomerRepairRequestTimelineItemResponse> getRequestTimeline(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId) {
        return facade.getRequestTimeline(actor, requestId);
    }

    @PostMapping("/{requestId}/review")
    @Operation(
            summary = "Submit Customer Repair Review",
            description = "Submits a rating and optional comment for a completed repair request. "
                    + "Requires ROLE_CUSTOMER. Only completed repair requests owned by the customer can be reviewed. "
                    + "Each repair request can be reviewed at most once.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid rating or oversized comment"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive"),
            @ApiResponse(responseCode = "404", description = "Repair request not found"),
            @ApiResponse(responseCode = "409", description = "Request not completed, review already exists, or assignment not resolved")
    })
    public ResponseEntity<CustomerReviewResponse> submitReview(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId,
            @Valid @RequestBody CustomerReviewCreateRequest request) {
        CustomerReviewResponse response = facade.submitReview(actor, requestId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
