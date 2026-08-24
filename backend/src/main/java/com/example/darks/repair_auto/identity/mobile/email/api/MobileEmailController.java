package com.example.darks.repair_auto.identity.mobile.email.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.email.EmailVerificationService;
import com.example.darks.repair_auto.identity.mobile.email.dto.EmailVerificationConfirmRequest;
import com.example.darks.repair_auto.identity.mobile.email.dto.EmailVerificationRequest;
import com.example.darks.repair_auto.identity.mobile.email.dto.EmailVerificationResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/me/email")
public class MobileEmailController {

    private final EmailVerificationService emailVerificationService;

    public MobileEmailController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/request-verification")
    public EmailVerificationResponse request(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @Valid @RequestBody EmailVerificationRequest request) {
        return emailVerificationService.request(actor, request);
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verify(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @Valid @RequestBody EmailVerificationConfirmRequest request) {
        emailVerificationService.verify(actor, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthenticatedMobileActor actor) {
        emailVerificationService.remove(actor);
        return ResponseEntity.noContent().build();
    }
}
