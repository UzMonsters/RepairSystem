package com.example.darks.repair_auto.identity.mobile.profile.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.profile.api.dto.MobileProfilePatchRequest;
import com.example.darks.repair_auto.identity.mobile.profile.api.dto.MobileProfileResponse;
import com.example.darks.repair_auto.identity.mobile.profile.application.MobileProfileService;
import com.example.darks.repair_auto.profile.api.dto.AvatarResponse;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentDownload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/mobile/me")
@Tag(name = "Mobile Profile", description = "Self-service profile endpoints for authenticated Customer and Technician mobile actors")
@SecurityRequirement(name = "bearerAuth")
public class MobileProfileController {

    private final MobileProfileService mobileProfileService;

    public MobileProfileController(MobileProfileService mobileProfileService) {
        this.mobileProfileService = mobileProfileService;
    }

    @GetMapping({"", "/profile"})
    @Operation(
            summary = "Get Current Mobile Actor Profile",
            description = "Returns profile and account status for the currently authenticated Customer or Technician. "
                    + "Actor identity is strictly resolved from the bearer JWT principal.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive")
    })
    public MobileProfileResponse me(@AuthenticationPrincipal AuthenticatedMobileActor actor) {
        return mobileProfileService.getProfile(actor);
    }

    @PatchMapping({"", "/profile"})
    @Operation(
            summary = "Update Current Mobile Actor Profile",
            description = "Partially updates self-service profile fields. For Customer: fullName and preferredLanguage. "
                    + "For Technician: preferredLanguage. Identity-sensitive fields such as phone and Telegram link remain read-only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or unsupported language"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive")
    })
    public MobileProfileResponse updateProfile(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @Valid @RequestBody MobileProfilePatchRequest request) {
        return mobileProfileService.updateProfile(actor, request);
    }

    @GetMapping("/avatar")
    @Operation(summary = "Get mobile self avatar image stream", description = "Streams avatar image for current authenticated mobile actor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar image stream returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Avatar not found")
    })
    public ResponseEntity<InputStreamResource> getAvatar(@AuthenticationPrincipal AuthenticatedMobileActor actor) {
        AttachmentDownload download = mobileProfileService.downloadAvatar(actor);
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

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload mobile self avatar", description = "Uploads or replaces avatar for current authenticated mobile actor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or unsupported image format"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "503", description = "Storage operation failed")
    })
    public AvatarResponse uploadAvatar(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @RequestParam("file") MultipartFile file) {
        return mobileProfileService.uploadAvatar(actor, file);
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Remove mobile self avatar", description = "Deletes avatar for current authenticated mobile actor.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Avatar removed successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal AuthenticatedMobileActor actor) {
        mobileProfileService.deleteAvatar(actor);
        return ResponseEntity.noContent().build();
    }
}
