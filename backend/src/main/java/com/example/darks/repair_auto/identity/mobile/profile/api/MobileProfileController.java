package com.example.darks.repair_auto.identity.mobile.profile.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.profile.api.dto.MobileProfilePatchRequest;
import com.example.darks.repair_auto.identity.mobile.profile.api.dto.MobileProfileResponse;
import com.example.darks.repair_auto.identity.mobile.profile.application.MobileProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/me")
@Tag(name = "Mobile Profile", description = "Self-service profile endpoints for authenticated Customer and Technician mobile actors")
@SecurityRequirement(name = "bearerAuth")
public class MobileProfileController {

    private final MobileProfileService mobileProfileService;

    public MobileProfileController(MobileProfileService mobileProfileService) {
        this.mobileProfileService = mobileProfileService;
    }

    @GetMapping
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

    @PatchMapping
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
}
