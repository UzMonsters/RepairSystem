package com.example.darks.repair_auto.technician.api;

import com.example.darks.repair_auto.shared.pagination.PageResponse;
import com.example.darks.repair_auto.technician.api.dto.TechnicianActivationRequest;
import com.example.darks.repair_auto.technician.api.dto.TechnicianCreateRequest;
import com.example.darks.repair_auto.technician.api.dto.TechnicianDetailResponse;
import com.example.darks.repair_auto.technician.api.dto.TechnicianSummaryResponse;
import com.example.darks.repair_auto.technician.api.dto.TechnicianUpdateRequest;
import com.example.darks.repair_auto.technician.application.TechnicianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/technicians")
@SecurityRequirement(name = "bearerAuth")
public class TechnicianController {

    private final TechnicianService technicianService;

    public TechnicianController(TechnicianService technicianService) {
        this.technicianService = technicianService;
    }

    @GetMapping
    @Operation(summary = "List technicians", description = "Requires ADMIN or MANAGER. Page size is limited to 100.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technicians returned"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, page, size, or sort"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required")
    })
    public PageResponse<TechnicianSummaryResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean telegramLinked,
            @Parameter(description = "Zero-based page index. Default: 0.")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size from 1 to 100. Default: 20.")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Sort fields: id, fullName, phone, specialization, maximumConcurrentRequests, active, createdAt, updatedAt.")
            @RequestParam(required = false) List<String> sort) {
        return technicianService.list(
                search,
                phone,
                specialization,
                active,
                telegramLinked,
                TechnicianPageRequest.toPageable(page, size, sort));
    }

    @GetMapping("/{id}")
    public TechnicianDetailResponse get(@PathVariable Long id) {
        return technicianService.get(id);
    }

    @PostMapping
    public TechnicianDetailResponse create(@Valid @RequestBody TechnicianCreateRequest request) {
        return technicianService.create(request);
    }

    @PutMapping("/{id}")
    public TechnicianDetailResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TechnicianUpdateRequest request) {
        return technicianService.update(id, request);
    }

    @PatchMapping("/{id}/activation")
    public TechnicianDetailResponse changeActivation(
            @PathVariable Long id,
            @Valid @RequestBody TechnicianActivationRequest request) {
        return technicianService.changeActivation(id, request.active(), request.reason());
    }
}
