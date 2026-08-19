package com.example.darks.repair_auto.notification.push.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.notification.push.api.dto.PushEndpointRegisterRequest;
import com.example.darks.repair_auto.notification.push.api.dto.PushEndpointResponse;
import com.example.darks.repair_auto.notification.push.api.dto.PushEndpointUnregisterRequest;
import com.example.darks.repair_auto.notification.push.application.PushEndpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/push-endpoints")
@Tag(name = "Staff Push Endpoints", description = "Push notification endpoint registration for Admin and Manager web clients")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class StaffPushEndpointController {

    private final PushEndpointService service;

    public StaffPushEndpointController(PushEndpointService service) {
        this.service = service;
    }

    @PutMapping
    @Operation(
            summary = "Register or refresh Staff Web push endpoint",
            description = "Registers or updates a Firebase push installation ID for the currently authenticated Admin or Manager. "
                    + "Registration is idempotent and reassigns installation ownership if the device was previously registered to another account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Push endpoint registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or incompatible client/platform/app combination"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account disabled")
    })
    public PushEndpointResponse register(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PushEndpointRegisterRequest request) {
        return service.registerForStaff(user.id(), request);
    }

    @DeleteMapping
    @Operation(
            summary = "Unregister or disable Staff Web push endpoint",
            description = "Disables push notification delivery for the specified Firebase installation ID if owned by the current staff user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Push endpoint unregistered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PushEndpointUnregisterRequest request) {
        service.unregisterForStaff(user.id(), request);
        return ResponseEntity.noContent().build();
    }
}
