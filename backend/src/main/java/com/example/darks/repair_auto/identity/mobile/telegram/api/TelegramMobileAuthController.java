package com.example.darks.repair_auto.identity.mobile.telegram.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.telegram.TelegramMobileAuthService;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.MobileAuthResponse;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.MobileLogoutRequest;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.MobileRefreshRequest;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.TelegramLoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/auth")
@Tag(name = "Mobile Authentication", description = "Endpoints for native mobile Telegram OIDC authentication, refresh tokens, and session management")
public class TelegramMobileAuthController {

    private final TelegramMobileAuthService authService;

    public TelegramMobileAuthController(TelegramMobileAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/telegram/customer")
    @Operation(
            summary = "Authenticate Customer via Telegram OIDC ID Token",
            description = "Authenticates a Customer using the signed OIDC ID token returned by the official "
                    + "Telegram Login SDK for Customer Mobile App. Validates cryptographic signature, issuer, "
                    + "customer audience, expiration, and resolves the linked Customer account to issue a "
                    + "RepairAuto Customer access JWT and rotating opaque refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer authenticated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing token"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, audience-mismatched, or unlinked Telegram token"),
            @ApiResponse(responseCode = "403", description = "Customer account is inactive")
    })
    public MobileAuthResponse loginCustomer(@Valid @RequestBody TelegramLoginRequest request) {
        return authService.loginCustomer(request.idToken());
    }

    @PostMapping("/telegram/technician")
    @Operation(
            summary = "Authenticate Technician via Telegram OIDC ID Token",
            description = "Authenticates a Technician using the signed OIDC ID token returned by the official "
                    + "Telegram Login SDK for Technician Mobile App. Validates cryptographic signature, issuer, "
                    + "technician audience, expiration, and resolves the linked Technician account to issue a "
                    + "RepairAuto Technician access JWT and rotating opaque refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technician authenticated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing token"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, audience-mismatched, or unlinked Telegram token"),
            @ApiResponse(responseCode = "403", description = "Technician account is inactive")
    })
    public MobileAuthResponse loginTechnician(@Valid @RequestBody TelegramLoginRequest request) {
        return authService.loginTechnician(request.idToken());
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Rotate Mobile Refresh Token",
            description = "Consumes a single-use opaque mobile refresh token, verifies the active actor state, "
                    + "rotates the session, and returns a replacement access token and new opaque refresh token. "
                    + "If an already-rotated token is presented (reuse detected), the entire token family is revoked.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token rotated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing token"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or reused refresh token"),
            @ApiResponse(responseCode = "403", description = "Actor account is inactive")
    })
    public MobileAuthResponse refresh(@Valid @RequestBody MobileRefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Log Out Single Mobile Session",
            description = "Revokes the presented mobile refresh token and its session family. "
                    + "Operation is idempotent and does not require an active access JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Session logged out successfully")
    })
    public ResponseEntity<Void> logout(@Valid @RequestBody MobileLogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @Operation(
            summary = "Log Out All Mobile Sessions for Current Actor",
            description = "Revokes all active mobile refresh sessions belonging to the authenticated Customer or Technician.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "All mobile sessions revoked successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AuthenticatedMobileActor actor) {
        authService.logoutAll(actor);
        return ResponseEntity.noContent().build();
    }
}
