package com.example.darks.repair_auto.auth.web;

import com.example.darks.repair_auto.auth.dto.LoginRequest;
import com.example.darks.repair_auto.auth.dto.LoginResponse;
import com.example.darks.repair_auto.auth.dto.LogoutRequest;
import com.example.darks.repair_auto.auth.dto.PasswordChangeRequest;
import com.example.darks.repair_auto.auth.dto.RefreshRequest;
import com.example.darks.repair_auto.auth.dto.TokenResponse;
import com.example.darks.repair_auto.auth.service.AuthenticationService;
import com.example.darks.repair_auto.security.AuthenticatedUser;
import com.example.darks.repair_auto.user.domain.UserRepository;
import com.example.darks.repair_auto.user.dto.UserDetailsResponse;
import com.example.darks.repair_auto.user.dto.UserMapper;
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
    private final UserRepository userRepository;

    public AuthController(AuthenticationService authenticationService, UserRepository userRepository) {
        this.authenticationService = authenticationService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return authenticationService.login(
                request.email(),
                request.password(),
                clientIp(servletRequest),
                userAgent(servletRequest));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest servletRequest) {
        return authenticationService.refresh(
                request.refreshToken(),
                clientIp(servletRequest),
                userAgent(servletRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authenticationService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AuthenticatedUser user) {
        authenticationService.logoutAll(user.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public UserDetailsResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return UserMapper.details(userRepository.findById(user.id()).orElseThrow());
    }

    @PatchMapping("/password")
    @SecurityRequirement(name = "bearerAuth")
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
