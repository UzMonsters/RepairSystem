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
@RequestMapping("/api/v1/mobile/me/jobs/{requestId}/attachments")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mobile Attachments", description = "Technician mobile job attachment operations")
public class TechnicianMobileAttachmentController {

    private final MobileAttachmentFacade facade;

    public TechnicianMobileAttachmentController(MobileAttachmentFacade facade) {
        this.facade = facade;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(
            summary = "Upload technician job attachment",
            description = "Uploads a diagnosis or completion photo using multipart/form-data. Requires ROLE_TECHNICIAN."
    )
    public MobileAttachmentResponse uploadAttachment(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId,
            @RequestParam(name = "attachmentType") AttachmentType attachmentType,
            @RequestPart("file") MultipartFile file) {
        return facade.uploadTechnicianAttachment(actor, requestId, attachmentType, file);
    }

    @GetMapping
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(
            summary = "List technician job attachments",
            description = "Lists available attachments for the assigned repair job. Requires ROLE_TECHNICIAN."
    )
    public List<MobileAttachmentResponse> listAttachments(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable Long requestId) {
        return facade.listTechnicianAttachments(actor, requestId);
    }
}
