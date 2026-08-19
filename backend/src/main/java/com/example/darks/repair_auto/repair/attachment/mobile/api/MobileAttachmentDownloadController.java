package com.example.darks.repair_auto.repair.attachment.mobile.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.attachment.mobile.api.dto.MobileAttachmentDownloadUrlResponse;
import com.example.darks.repair_auto.repair.attachment.mobile.application.MobileAttachmentFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/me/attachments/{attachmentId}/download-url")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mobile Attachments", description = "Mobile attachment download URL operations")
public class MobileAttachmentDownloadController {

    private final MobileAttachmentFacade facade;

    public MobileAttachmentDownloadController(MobileAttachmentFacade facade) {
        this.facade = facade;
    }

    @GetMapping
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
        return facade.getDownloadUrl(actor, attachmentId);
    }
}
