package com.example.darks.repair_auto.notification.push.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
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
@RequestMapping("/api/v1/mobile/me/push-endpoints")
@Tag(name = "Mobile Push Endpoints", description = "Push notification endpoint registration for Customer and Technician mobile apps")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN')")
public class MobilePushEndpointController {

    private final PushEndpointService service;

    public MobilePushEndpointController(PushEndpointService service) {
        this.service = service;
    }

    @PutMapping
    @Operation(
            summary = "Register or refresh Mobile push endpoint",
            description = "Registers or updates a Firebase push installation ID for the currently authenticated Customer or Technician. "
                    + "Registration is idempotent and reassigns installation ownership if the physical device was previously registered to another account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Push endpoint registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or incompatible client/platform/app combination"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive")
    })
    public PushEndpointResponse register(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @Valid @RequestBody PushEndpointRegisterRequest request) {
        return service.registerForMobile(actor, request);
    }

    @DeleteMapping
    @Operation(
            summary = "Unregister or disable Mobile push endpoint",
            description = "Disables push notification delivery for the specified Firebase installation ID if owned by the current mobile actor.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Push endpoint unregistered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @Valid @RequestBody PushEndpointUnregisterRequest request) {
        service.unregisterForMobile(actor, request);
        return ResponseEntity.noContent().build();
    }
}
