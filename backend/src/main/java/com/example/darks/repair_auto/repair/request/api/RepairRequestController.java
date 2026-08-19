package com.example.darks.repair_auto.repair.request.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateRequest;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateResponse;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestDetailResponse;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestSummaryResponse;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestUpdateRequest;
import com.example.darks.repair_auto.repair.request.application.RepairRequestQuery;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestSource;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@SecurityRequirement(name = "bearerAuth")
public class RepairRequestController {

    private final RepairRequestService repairRequestService;

    public RepairRequestController(RepairRequestService repairRequestService) {
        this.repairRequestService = repairRequestService;
    }

    @GetMapping({"/api/v1/requests", "/api/v1/repair-requests"})
    @Operation(
            summary = "List repair requests",
            description = """
                    Requires ADMIN or MANAGER. New REST-created requests always start as NEW. Status and source are \
                    backend-controlled; repair execution, attachments, and Telegram workflow adapters are deferred.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repair requests returned"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, date range, page, size, or sort"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required")
    })
    public PageResponse<RepairRequestSummaryResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String requestNumber,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) RepairRequestStatus status,
            @RequestParam(required = false) RepairRequestPriority priority,
            @RequestParam(required = false) RepairRequestSource source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime createdTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime preferredVisitFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime preferredVisitTo,
            @Parameter(description = "Zero-based page index. Default: 0.")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size from 1 to 100. Default: 20.")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Sort fields: id, requestNumber, priority, status, source, customerPreferredVisitAt, createdAt, updatedAt, customerName, categoryName.")
            @RequestParam(required = false) List<String> sort) {
        return repairRequestService.list(
                query(
                        search,
                        requestNumber,
                        customerId,
                        categoryId,
                        status,
                        priority,
                        source,
                        createdFrom,
                        createdTo,
                        preferredVisitFrom,
                        preferredVisitTo),
                RepairRequestPageRequest.toPageable(page, size, sort));
    }

    @GetMapping({"/api/v1/requests/{id}", "/api/v1/repair-requests/{id}"})
    @Operation(summary = "Get repair request details", description = "Requires ADMIN or MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repair request returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required"),
            @ApiResponse(responseCode = "404", description = "REPAIR_REQUEST_NOT_FOUND")
    })
    public RepairRequestDetailResponse get(@PathVariable Long id) {
        return repairRequestService.get(id);
    }

    @PostMapping({"/api/v1/requests", "/api/v1/repair-requests"})
    @Operation(
            summary = "Create repair request",
            description = """
                    Requires ADMIN or MANAGER. The backend generates requestNumber, status=NEW, source=ADMIN, and \
                    createdByUserId from the authenticated user. customerPreferredVisitAt is only a customer preference.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repair request created"),
            @ApiResponse(responseCode = "400", description = "Invalid description, location, preferred visit time, or body"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required"),
            @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND or CATEGORY_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "REPAIR_REQUEST_CUSTOMER_INACTIVE or REPAIR_REQUEST_CATEGORY_INACTIVE")
    })
    public RepairRequestCreateResponse create(
            @Valid @RequestBody RepairRequestCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return repairRequestService.create(request, user);
    }

    @PutMapping({"/api/v1/requests/{id}", "/api/v1/repair-requests/{id}"})
    @Operation(
            summary = "Update repair request intake",
            description = "Requires ADMIN or MANAGER. Only NEW requests are editable; status and source remain unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repair request updated"),
            @ApiResponse(responseCode = "400", description = "Invalid description, location, preferred visit time, or body"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required"),
            @ApiResponse(responseCode = "404", description = "REPAIR_REQUEST_NOT_FOUND, CUSTOMER_NOT_FOUND, or CATEGORY_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "REPAIR_REQUEST_NOT_EDITABLE, inactive customer/category, or optimistic lock conflict")
    })
    public RepairRequestDetailResponse update(
            @PathVariable Long id,
            @Valid @RequestBody RepairRequestUpdateRequest request) {
        return repairRequestService.update(id, request);
    }

    @DeleteMapping({"/api/v1/requests/{id}", "/api/v1/repair-requests/{id}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Soft delete repair request",
            description = "Requires ADMIN or MANAGER. The request is hidden from normal lists and reads, but retained for audit.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Repair request soft deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required"),
            @ApiResponse(responseCode = "404", description = "REPAIR_REQUEST_NOT_FOUND")
    })
    public void softDelete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        repairRequestService.softDelete(id, user);
    }

    @GetMapping({"/api/v1/customers/{customerId}/requests", "/api/v1/customers/{customerId}/repair-requests"})
    @Operation(summary = "List a customer's repair history", description = "Requires ADMIN or MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer repair history returned"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, date range, page, size, or sort"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required"),
            @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND")
    })
    public PageResponse<RepairRequestSummaryResponse> customerHistory(
            @PathVariable Long customerId,
            @RequestParam(required = false) RepairRequestStatus status,
            @RequestParam(required = false) RepairRequestPriority priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime createdTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {
        return repairRequestService.customerHistory(
                customerId,
                query(null, null, null, categoryId, status, priority, null, createdFrom, createdTo, null, null),
                RepairRequestPageRequest.toPageable(page, size, sort));
    }

    private RepairRequestQuery query(
            String search,
            String requestNumber,
            Long customerId,
            Long categoryId,
            RepairRequestStatus status,
            RepairRequestPriority priority,
            RepairRequestSource source,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo,
            OffsetDateTime preferredVisitFrom,
            OffsetDateTime preferredVisitTo) {
        return new RepairRequestQuery(
                search,
                requestNumber,
                customerId,
                categoryId,
                status,
                priority,
                source,
                createdFrom,
                createdTo,
                preferredVisitFrom,
                preferredVisitTo);
    }
}
