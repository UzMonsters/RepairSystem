package com.example.darks.repair_auto.repair.assignment.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentDetailResponse;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRejectionRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.ReassignmentRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.ScheduleRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.TechnicianWorkloadResponse;
import com.example.darks.repair_auto.repair.assignment.api.dto.UnassignmentRequest;
import com.example.darks.repair_auto.repair.assignment.application.RepairAssignmentService;
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
public class RepairAssignmentController {

    private final RepairAssignmentService repairAssignmentService;

    public RepairAssignmentController(RepairAssignmentService repairAssignmentService) {
        this.repairAssignmentService = repairAssignmentService;
    }

    @PostMapping("/api/v1/requests/{requestId}/assign")
    @Operation(summary = "Assign a technician", description = "Requires ADMIN or MANAGER. Creates a PENDING assignment.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request assigned"),
            @ApiResponse(responseCode = "400", description = "Invalid schedule timestamp or body"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required"),
            @ApiResponse(responseCode = "404", description = "REPAIR_REQUEST_NOT_FOUND or TECHNICIAN_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "Assignment state or technician capacity conflict")
    })
    public RepairRequestDetailResponse assign(
            @PathVariable Long requestId,
            @Valid @RequestBody AssignmentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return repairAssignmentService.assign(requestId, request, user);
    }

    @PostMapping("/api/v1/requests/{requestId}/reassign")
    @Operation(summary = "Reassign a request", description = "Requires ADMIN or MANAGER. Closes the active assignment as REASSIGNED.")
    public RepairRequestDetailResponse reassign(
            @PathVariable Long requestId,
            @Valid @RequestBody ReassignmentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return repairAssignmentService.reassign(requestId, request, user);
    }

    @PostMapping("/api/v1/requests/{requestId}/unassign")
    @Operation(summary = "Unassign a request", description = "Requires ADMIN or MANAGER. Request returns to NEW.")
    public RepairRequestDetailResponse unassign(
            @PathVariable Long requestId,
            @Valid @RequestBody UnassignmentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return repairAssignmentService.unassign(requestId, request, user);
    }

    @PatchMapping("/api/v1/requests/{requestId}/schedule")
    @Operation(summary = "Schedule, reschedule, or clear the active assignment visit time")
    public RepairRequestDetailResponse schedule(
            @PathVariable Long requestId,
            @RequestBody ScheduleRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return repairAssignmentService.schedule(requestId, request, user);
    }

    @PostMapping("/api/v1/requests/{requestId}/assignment/accept")
    @Operation(summary = "Accept the active assignment", description = "Staff-authorized Phase 4 workflow endpoint.")
    public RepairRequestDetailResponse accept(
            @PathVariable Long requestId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return repairAssignmentService.accept(requestId, user);
    }

    @PostMapping("/api/v1/requests/{requestId}/assignment/reject")
    @Operation(summary = "Reject the active assignment", description = "Staff-authorized Phase 4 workflow endpoint.")
    public RepairRequestDetailResponse reject(
            @PathVariable Long requestId,
            @Valid @RequestBody AssignmentRejectionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return repairAssignmentService.reject(requestId, request, user);
    }

    @GetMapping("/api/v1/requests/{requestId}/assignments")
    @Operation(summary = "List assignment history", description = "Requires ADMIN or MANAGER. Newest first.")
    public List<AssignmentDetailResponse> history(@PathVariable Long requestId) {
        return repairAssignmentService.history(requestId);
    }

    @GetMapping("/api/v1/technicians/{technicianId}/workload")
    @Operation(summary = "Get technician workload", description = "Requires ADMIN or MANAGER.")
    public TechnicianWorkloadResponse workload(@PathVariable Long technicianId) {
        return repairAssignmentService.workload(technicianId);
    }
}
