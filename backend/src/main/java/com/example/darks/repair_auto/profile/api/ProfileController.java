package com.example.darks.repair_auto.profile.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.profile.api.dto.AvatarResponse;
import com.example.darks.repair_auto.profile.api.dto.ProfileResponse;
import com.example.darks.repair_auto.profile.api.dto.UpdateProfileRequest;
import com.example.darks.repair_auto.profile.application.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentDownload;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
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
@RequestMapping("/api/v1/me")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Get current authenticated user profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user profile returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ProfileResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return profileService.getCurrentProfile(user.id());
    }

    @PatchMapping
    @Operation(summary = "Update current user self-service profile and preferences")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or phone format"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ProfileResponse updateProfile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateCurrentProfile(user.id(), request);
    }

    @GetMapping("/avatar")
    @Operation(summary = "Get current authenticated user avatar image stream")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user avatar image stream returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Avatar not found")
    })
    public ResponseEntity<InputStreamResource> getAvatar(@AuthenticationPrincipal AuthenticatedUser user) {
        AttachmentDownload download = profileService.downloadAvatar(user.id());
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
    @Operation(summary = "Upload or replace current user avatar")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or unsupported image format"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "503", description = "Storage operation failed")
    })
    public AvatarResponse uploadAvatar(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Avatar image file (JPEG, PNG, WebP)")
            @RequestParam("file") MultipartFile file) {
        return profileService.uploadAvatar(user.id(), file);
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Remove current user avatar")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Avatar removed successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal AuthenticatedUser user) {
        profileService.deleteAvatar(user.id());
        return ResponseEntity.noContent().build();
    }
}
