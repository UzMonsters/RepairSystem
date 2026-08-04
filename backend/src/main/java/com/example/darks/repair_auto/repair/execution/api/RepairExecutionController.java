package com.example.darks.repair_auto.repair.execution.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.repair.execution.api.dto.CancelRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.CompleteRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.DiagnosisRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.RepairExecutionDetailResponse;
import com.example.darks.repair_auto.repair.execution.api.dto.RepairRequestStatusHistoryResponse;
import com.example.darks.repair_auto.repair.execution.api.dto.ResumeRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.WaitForPartsRequest;
import com.example.darks.repair_auto.repair.execution.application.RepairExecutionService;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class RepairExecutionController {

    private final RepairExecutionService repairExecutionService;

    public RepairExecutionController(RepairExecutionService repairExecutionService) {
        this.repairExecutionService = repairExecutionService;
    }

    @PostMapping("/api/v1/requests/{requestId}/start")
    @Operation(summary = "Start repair work", description = "Requires ADMIN or MANAGER and an accepted assignment.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repair started"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required"),
            @ApiResponse(responseCode = "404", description = "REPAIR_REQUEST_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "Invalid lifecycle state or concurrency conflict")
    })
    public RepairRequestDetailResponse start(
            @PathVariable Long requestId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return repairExecutionService.start(requestId, user);
    }

    @PatchMapping("/api/v1/requests/{requestId}/diagnosis")
    @Operation(summary = "Create or update diagnosis", description = "Supports EN, RU, and UZ Unicode plain text.")
    public RepairExecutionDetailResponse diagnosis(
            @PathVariable Long requestId,
            @Valid @RequestBody DiagnosisRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return repairExecutionService.updateDiagnosis(requestId, request, user);
    }

    @PostMapping("/api/v1/requests/{requestId}/wait-for-parts")
    @Operation(summary = "Move repair to waiting for parts")
    public RepairRequestDetailResponse waitForParts(
            @PathVariable Long requestId,
            @Valid @RequestBody WaitForPartsRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return repairExecutionService.waitForParts(requestId, request, user);
    }

    @PostMapping("/api/v1/requests/{requestId}/resume")
    @Operation(summary = "Resume repair from waiting for parts")
    public RepairRequestDetailResponse resume(
            @PathVariable Long requestId,
            @Valid @RequestBody ResumeRepairRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return repairExecutionService.resume(requestId, request, user);
    }

    @PostMapping("/api/v1/requests/{requestId}/complete")
    @Operation(summary = "Complete repair work", description = "Requires diagnosis and work performed. Photos are Phase 6.")
    public RepairRequestDetailResponse complete(
            @PathVariable Long requestId,
            @Valid @RequestBody CompleteRepairRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return repairExecutionService.complete(requestId, request, user);
    }

    @PostMapping("/api/v1/requests/{requestId}/cancel")
    @Operation(summary = "Cancel repair request", description = "Allowed until request reaches a terminal status.")
    public RepairRequestDetailResponse cancel(
            @PathVariable Long requestId,
            @Valid @RequestBody CancelRepairRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return repairExecutionService.cancel(requestId, request, user);
    }

    @GetMapping("/api/v1/requests/{requestId}/execution")
    @Operation(summary = "Get repair execution details")
    public RepairExecutionDetailResponse execution(@PathVariable Long requestId) {
        return repairExecutionService.getExecution(requestId);
    }

    @GetMapping("/api/v1/requests/{requestId}/status-history")
    @Operation(summary = "Get append-only status history", description = "Newest first.")
    public List<RepairRequestStatusHistoryResponse> statusHistory(@PathVariable Long requestId) {
        return repairExecutionService.statusHistory(requestId);
    }
}
