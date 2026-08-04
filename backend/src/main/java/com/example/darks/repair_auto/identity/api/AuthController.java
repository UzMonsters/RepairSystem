package com.example.darks.repair_auto.identity.api;

import com.example.darks.repair_auto.identity.api.dto.LoginRequest;
import com.example.darks.repair_auto.identity.api.dto.LoginResponse;
import com.example.darks.repair_auto.identity.api.dto.LogoutRequest;
import com.example.darks.repair_auto.identity.api.dto.PasswordChangeRequest;
import com.example.darks.repair_auto.identity.api.dto.RefreshRequest;
import com.example.darks.repair_auto.identity.api.dto.TokenResponse;
import com.example.darks.repair_auto.identity.application.AuthenticationService;
import com.example.darks.repair_auto.identity.application.AuthThrottleService;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.api.dto.UserDetailsResponse;
import com.example.darks.repair_auto.identity.api.dto.UserMapper;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final AuthThrottleService authThrottleService;
    private final UserRepository userRepository;

    public AuthController(
            AuthenticationService authenticationService,
            AuthThrottleService authThrottleService,
            UserRepository userRepository) {
        this.authenticationService = authenticationService;
        this.authThrottleService = authThrottleService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String clientIp = clientIp(servletRequest);
        authThrottleService.checkLogin(request.email(), clientIp);
        try {
            LoginResponse response = authenticationService.login(
                    request.email(),
                    request.password(),
                    clientIp,
                    userAgent(servletRequest));
            authThrottleService.recordLoginSuccess(request.email(), clientIp);
            return response;
        } catch (BusinessRuleException exception) {
            if (exception.status() == 401) {
                authThrottleService.recordLoginFailure(request.email(), clientIp);
            }
            throw exception;
        }
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest servletRequest) {
        String clientIp = clientIp(servletRequest);
        authThrottleService.checkRefresh(request.refreshToken(), clientIp);
        try {
            TokenResponse response = authenticationService.refresh(
                    request.refreshToken(),
                    clientIp,
                    userAgent(servletRequest));
            authThrottleService.recordRefreshSuccess(request.refreshToken(), clientIp);
            return response;
        } catch (BusinessRuleException exception) {
            if (exception.status() == 401) {
                authThrottleService.recordRefreshFailure(request.refreshToken(), clientIp);
            }
            throw exception;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authenticationService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "All sessions revoked"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AuthenticatedUser user) {
        authenticationService.logoutAll(user.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public UserDetailsResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return UserMapper.details(userRepository.findById(user.id()).orElseThrow());
    }

    @PatchMapping("/password")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Invalid password change request"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PasswordChangeRequest request) {
        authenticationService.changePassword(user.id(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.length() <= 512) {
            return userAgent;
        }
        return userAgent.substring(0, 512);
    }
}
