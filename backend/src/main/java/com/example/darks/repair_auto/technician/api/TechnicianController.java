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

import com.example.darks.repair_auto.profile.api.dto.AvatarResponse;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentDownload;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/{id}/avatar")
    @Operation(summary = "Get technician avatar image stream", description = "Streams technician avatar. Requires ADMIN or MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technician avatar returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required"),
            @ApiResponse(responseCode = "404", description = "Technician or avatar not found")
    })
    public ResponseEntity<InputStreamResource> getAvatar(@PathVariable Long id) {
        AttachmentDownload download = technicianService.downloadAvatar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(download.fileName())
                        .build()
                        .toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(new InputStreamResource(download.inputStream()));
    }

    @PutMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace technician avatar", description = "Requires ADMIN or MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or unsupported image format"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required"),
            @ApiResponse(responseCode = "404", description = "Technician not found"),
            @ApiResponse(responseCode = "503", description = "Storage operation failed")
    })
    public AvatarResponse uploadAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return technicianService.uploadAvatar(id, file);
    }

    @DeleteMapping("/{id}/avatar")
    @Operation(summary = "Remove technician avatar", description = "Requires ADMIN or MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Avatar removed successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required"),
            @ApiResponse(responseCode = "404", description = "Technician not found")
    })
    public ResponseEntity<Void> deleteAvatar(@PathVariable Long id) {
        technicianService.deleteAvatar(id);
        return ResponseEntity.noContent().build();
    }
}
