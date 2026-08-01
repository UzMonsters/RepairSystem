package com.example.darks.repair_auto.user.web;

import com.example.darks.repair_auto.common.api.PageResponse;
import com.example.darks.repair_auto.security.AuthenticatedUser;
import com.example.darks.repair_auto.user.domain.UserRole;
import com.example.darks.repair_auto.user.dto.UserActivationRequest;
import com.example.darks.repair_auto.user.dto.UserCreateRequest;
import com.example.darks.repair_auto.user.dto.UserDetailsResponse;
import com.example.darks.repair_auto.user.dto.UserRoleChangeRequest;
import com.example.darks.repair_auto.user.dto.UserSummaryResponse;
import com.example.darks.repair_auto.user.dto.UserUpdateRequest;
import com.example.darks.repair_auto.user.service.UserManagementService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public PageResponse<UserSummaryResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return userManagementService.list(search, role, active, pageable);
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
}
