package com.example.darks.repair_auto.repair.attachment.mobile.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.mobile.api.dto.MobileAttachmentResponse;
import com.example.darks.repair_auto.repair.attachment.mobile.application.MobileAttachmentFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/mobile/me/repair-requests/{requestId}/attachments")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mobile Attachments", description = "Customer mobile attachment operations")
public class CustomerMobileAttachmentController {

    private final MobileAttachmentFacade facade;

    public CustomerMobileAttachmentController(MobileAttachmentFacade facade) {
        this.facade = facade;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Upload customer repair request attachment",
            description = "Uploads a customer problem photo using multipart/form-data. Requires ROLE_CUSTOMER."
    )
    public MobileAttachmentResponse uploadAttachment(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId,
            @RequestParam(name = "attachmentType", required = false) AttachmentType attachmentType,
            @RequestPart("file") MultipartFile file) {
        return facade.uploadCustomerAttachment(actor, requestId, attachmentType, file);
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "List customer repair request attachments",
            description = "Lists available attachments for the authenticated customer's repair request. Requires ROLE_CUSTOMER."
    )
    public List<MobileAttachmentResponse> listAttachments(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId) {
        return facade.listCustomerAttachments(actor, requestId);
    }
}
