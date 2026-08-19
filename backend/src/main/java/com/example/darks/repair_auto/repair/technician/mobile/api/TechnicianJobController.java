package com.example.darks.repair_auto.repair.technician.mobile.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRejectionRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.CompleteRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.DiagnosisRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.ResumeRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.WaitForPartsRequest;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianJobDetailResponse;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianJobListView;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianJobSummaryResponse;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianScheduleItemResponse;
import com.example.darks.repair_auto.repair.technician.mobile.application.TechnicianJobFacade;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/me")
@Tag(name = "Technician Mobile Jobs", description = "Endpoints for Technician self-service job and repair execution management")
@SecurityRequirement(name = "bearerAuth")
public class TechnicianJobController {

    private final TechnicianJobFacade facade;

    public TechnicianJobController(TechnicianJobFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/jobs")
    @Operation(
            summary = "List Assigned Jobs",
            description = "Returns paginated list of jobs assigned to the authenticated Technician. "
                    + "Supports ACTIVE (pending, accepted) and HISTORY (completed, cancelled) views.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of technician jobs returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive")
    })
    public PageResponse<TechnicianJobSummaryResponse> listJobs(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @Parameter(description = "Job view filter: ACTIVE (default) or HISTORY")
            @RequestParam(required = false, defaultValue = "ACTIVE") TechnicianJobListView view,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return facade.listJobs(actor, view, pageable);
    }

    @GetMapping("/jobs/{requestId}")
    @Operation(
            summary = "Get Job Detail",
            description = "Returns detailed information of an assigned repair job for the authenticated Technician. "
                    + "Returns 404 if the request is not found or the technician was rejected/unassigned/reassigned.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job details returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public TechnicianJobDetailResponse getJobDetail(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId) {
        return facade.getJobDetail(actor, requestId);
    }

    @GetMapping("/schedule")
    @Operation(
            summary = "Get Visit Schedule",
            description = "Returns list of active scheduled assignments for the authenticated Technician within the date range (up to 60 days).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedule items returned"),
            @ApiResponse(responseCode = "400", description = "Invalid date range"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive")
    })
    public List<TechnicianScheduleItemResponse> getSchedule(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @Parameter(description = "Start date (inclusive, YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (inclusive, YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return facade.getSchedule(actor, from, to);
    }

    @PostMapping("/jobs/{requestId}/accept")
    @Operation(
            summary = "Accept Job Assignment",
            description = "Accepts a pending assignment for the authenticated Technician and returns updated job details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job accepted successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive"),
            @ApiResponse(responseCode = "404", description = "Active assignment not found"),
            @ApiResponse(responseCode = "409", description = "Assignment already accepted or not pending")
    })
    public TechnicianJobDetailResponse acceptAssignment(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId) {
        return facade.acceptAssignment(actor, requestId);
    }

    @PostMapping("/jobs/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Reject Job Assignment",
            description = "Rejects a pending assignment with a mandatory reason. "
                    + "After rejection, the Technician loses access to the job detail.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Job rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid rejection reason"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive"),
            @ApiResponse(responseCode = "404", description = "Active assignment not found"),
            @ApiResponse(responseCode = "409", description = "Assignment already rejected or not pending")
    })
    public void rejectAssignment(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId,
            @Valid @RequestBody AssignmentRejectionRequest request) {
        facade.rejectAssignment(actor, requestId, request);
    }

    @PostMapping("/jobs/{requestId}/start")
    @Operation(
            summary = "Start Repair Execution",
            description = "Transitions an accepted job from ASSIGNED or SCHEDULED to IN_PROGRESS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repair started successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive"),
            @ApiResponse(responseCode = "404", description = "Active assignment not found"),
            @ApiResponse(responseCode = "409", description = "Repair not startable or already started")
    })
    public TechnicianJobDetailResponse startRepair(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId) {
        return facade.startRepair(actor, requestId);
    }

    @PatchMapping("/jobs/{requestId}/diagnosis")
    @Operation(
            summary = "Update Diagnosis",
            description = "Updates diagnostic findings for a repair currently IN_PROGRESS or WAITING_FOR_PARTS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Diagnosis updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid diagnosis payload"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive"),
            @ApiResponse(responseCode = "404", description = "Active assignment not found"),
            @ApiResponse(responseCode = "409", description = "Repair not in progress or waiting for parts")
    })
    public TechnicianJobDetailResponse updateDiagnosis(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId,
            @Valid @RequestBody DiagnosisRequest request) {
        return facade.updateDiagnosis(actor, requestId, request);
    }

    @PostMapping("/jobs/{requestId}/wait-for-parts")
    @Operation(
            summary = "Mark Repair as Waiting for Parts",
            description = "Transitions an IN_PROGRESS repair to WAITING_FOR_PARTS with a required reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repair marked as waiting for parts"),
            @ApiResponse(responseCode = "400", description = "Invalid waiting reason"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive"),
            @ApiResponse(responseCode = "404", description = "Active assignment not found"),
            @ApiResponse(responseCode = "409", description = "Repair not in progress")
    })
    public TechnicianJobDetailResponse waitForParts(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId,
            @Valid @RequestBody WaitForPartsRequest request) {
        return facade.waitForParts(actor, requestId, request);
    }

    @PostMapping("/jobs/{requestId}/resume")
    @Operation(
            summary = "Resume Repair Execution",
            description = "Transitions a repair from WAITING_FOR_PARTS back to IN_PROGRESS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repair resumed to in-progress"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive"),
            @ApiResponse(responseCode = "404", description = "Active assignment not found"),
            @ApiResponse(responseCode = "409", description = "Repair not waiting for parts")
    })
    public TechnicianJobDetailResponse resumeRepair(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId,
            @RequestBody(required = false) ResumeRepairRequest request) {
        return facade.resumeRepair(actor, requestId, request);
    }

    @PostMapping("/jobs/{requestId}/complete")
    @Operation(
            summary = "Complete Repair Execution",
            description = "Completes an IN_PROGRESS repair. Requires prior diagnosis, work performed description, and an available completion photo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repair completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid completion payload"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive"),
            @ApiResponse(responseCode = "404", description = "Active assignment not found"),
            @ApiResponse(responseCode = "409", description = "Repair already completed, diagnosis missing, or completion photo required")
    })
    public TechnicianJobDetailResponse completeRepair(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId,
            @Valid @RequestBody CompleteRepairRequest request) {
        return facade.completeRepair(actor, requestId, request);
    }
}
