package com.example.darks.repair_auto.repair.attachment.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.repair.attachment.api.dto.AttachmentDeleteRequest;
import com.example.darks.repair_auto.repair.attachment.api.dto.AttachmentResponse;
import com.example.darks.repair_auto.repair.attachment.api.dto.DownloadUrlResponse;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentService;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(
            value = "/api/v1/requests/{requestId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload repair attachment", description = "Requires ADMIN or MANAGER.")
    public AttachmentResponse upload(
            @PathVariable Long requestId,
            @RequestParam AttachmentType type,
            @RequestPart MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return attachmentService.upload(requestId, type, file, user);
    }

    @GetMapping("/api/v1/requests/{requestId}/attachments")
    @Operation(summary = "List available repair attachments", description = "Newest first. Requires ADMIN or MANAGER.")
    public List<AttachmentResponse> list(
            @PathVariable Long requestId,
            @RequestParam(required = false) AttachmentType type) {
        return attachmentService.list(requestId, type);
    }

    @GetMapping("/api/v1/attachments/{attachmentId}")
    @Operation(summary = "Get available attachment metadata", description = "Requires ADMIN or MANAGER.")
    public AttachmentResponse get(@PathVariable Long attachmentId) {
        return attachmentService.get(attachmentId);
    }

    @GetMapping("/api/v1/attachments/{attachmentId}/download-url")
    @Operation(summary = "Create short-lived attachment download URL", description = "Requires ADMIN or MANAGER.")
    public DownloadUrlResponse downloadUrl(@PathVariable Long attachmentId) {
        return attachmentService.downloadUrl(attachmentId);
    }

    @DeleteMapping("/api/v1/attachments/{attachmentId}")
    @Operation(summary = "Soft-delete available attachment", description = "Requires ADMIN or MANAGER.")
    public ResponseEntity<Void> delete(
            @PathVariable Long attachmentId,
            @Valid @RequestBody(required = false) AttachmentDeleteRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        attachmentService.delete(attachmentId, request, user);
        return ResponseEntity.noContent().build();
    }
}
