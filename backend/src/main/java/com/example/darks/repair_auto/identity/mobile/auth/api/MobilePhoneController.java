package com.example.darks.repair_auto.identity.mobile.auth.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.auth.dto.PhoneOtpResponse;
import com.example.darks.repair_auto.identity.mobile.auth.dto.PhoneVerificationConfirmRequest;
import com.example.darks.repair_auto.identity.mobile.auth.dto.PhoneVerificationRequest;
import com.example.darks.repair_auto.identity.mobile.otp.PhoneOtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/me/phone")
public class MobilePhoneController {

    private final PhoneOtpService phoneOtpService;

    public MobilePhoneController(PhoneOtpService phoneOtpService) {
        this.phoneOtpService = phoneOtpService;
    }

    @PostMapping("/request-verification")
    public PhoneOtpResponse requestVerification(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @Valid @RequestBody PhoneVerificationRequest request,
            HttpServletRequest httpServletRequest) {
        return phoneOtpService.requestVerificationForActor(
                actor,
                request,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader(HttpHeaders.USER_AGENT));
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verify(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @Valid @RequestBody PhoneVerificationConfirmRequest request) {
        phoneOtpService.verifyAndChangePhone(actor, actor.sessionId(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthenticatedMobileActor actor) {
        phoneOtpService.removePhone(actor, actor.sessionId());
        return ResponseEntity.noContent().build();
    }
}
