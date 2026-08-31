package com.example.darks.repair_auto.repair.attachment.mobile.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentDownload;
import com.example.darks.repair_auto.repair.attachment.mobile.api.dto.MobileAttachmentDownloadUrlResponse;
import com.example.darks.repair_auto.repair.attachment.mobile.application.MobileAttachmentFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/me/attachments")
@Tag(name = "Mobile Attachments", description = "Mobile actor attachment operations")
@SecurityRequirement(name = "bearerAuth")
public class MobileAttachmentDownloadController {

    private final MobileAttachmentFacade mobileAttachmentFacade;

    public MobileAttachmentDownloadController(MobileAttachmentFacade mobileAttachmentFacade) {
        this.mobileAttachmentFacade = mobileAttachmentFacade;
    }

    @GetMapping("/{attachmentId}/download-url")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN')")
    @Operation(
            summary = "Create short-lived temporary download URL for mobile attachment",
            description = "Generates a fresh temporary presigned download URL for an authorized attachment. "
                    + "Download URLs are temporary; store attachment IDs, not download URLs. "
                    + "Requires ROLE_CUSTOMER or ROLE_TECHNICIAN."
    )
    public MobileAttachmentDownloadUrlResponse getDownloadUrl(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long attachmentId) {
        return mobileAttachmentFacade.getDownloadUrl(actor, attachmentId);
    }

    @GetMapping("/{attachmentId}/download")
    @Operation(summary = "Download attachment content stream", description = "Streams the attachment file content for authorized mobile actors.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachment stream returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal AuthenticatedMobileActor actor) {
        AttachmentDownload download = mobileAttachmentFacade.downloadAttachment(actor, attachmentId);
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
}
