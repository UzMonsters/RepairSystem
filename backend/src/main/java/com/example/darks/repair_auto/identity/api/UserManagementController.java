package com.example.darks.repair_auto.identity.api;

import com.example.darks.repair_auto.shared.pagination.PageResponse;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.api.dto.UserActivationRequest;
import com.example.darks.repair_auto.identity.api.dto.UserCreateRequest;
import com.example.darks.repair_auto.identity.api.dto.UserDetailsResponse;
import com.example.darks.repair_auto.identity.api.dto.UserRoleChangeRequest;
import com.example.darks.repair_auto.identity.api.dto.UserSummaryResponse;
import com.example.darks.repair_auto.identity.api.dto.UserUpdateRequest;
import com.example.darks.repair_auto.identity.application.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @Operation(summary = "List users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users returned"),
            @ApiResponse(responseCode = "400", description = "Invalid page, size, or sort parameter"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required")
    })
    public PageResponse<UserSummaryResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Zero-based page index. Default: 0.")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size from 1 to 100. Default: 20.")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Sort expressions: id, fullName, email, role, active, createdAt, updatedAt, lastLoginAt.")
            @RequestParam(required = false) java.util.List<String> sort) {
        return userManagementService.list(search, role, active, UserPageRequest.toPageable(page, size, sort));
    }

    @GetMapping("/{id}")
    public UserDetailsResponse get(@PathVariable Long id) {
        return userManagementService.get(id);
    }

    @PostMapping
    public UserDetailsResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userManagementService.create(request);
    }

    @PutMapping("/{id}")
    public UserDetailsResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userManagementService.update(id, request);
    }

    @PatchMapping("/{id}/role")
    public UserDetailsResponse changeRole(@PathVariable Long id, @Valid @RequestBody UserRoleChangeRequest request) {
        return userManagementService.changeRole(id, request.role());
    }

    @PatchMapping("/{id}/activation")
    public UserDetailsResponse changeActivation(
            @PathVariable Long id,
            @Valid @RequestBody UserActivationRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userManagementService.changeActivation(id, request.active(), currentUser.id());
    }

    @PostMapping("/{id}/revoke-sessions")
    public ResponseEntity<Void> revokeSessions(@PathVariable Long id) {
        userManagementService.revokeSessions(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset target user's password (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Password confirmation mismatch or policy violation"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Target user not found")
    })
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody com.example.darks.repair_auto.identity.api.dto.ResetPasswordRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        userManagementService.resetPassword(id, request, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
