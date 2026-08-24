package com.example.darks.repair_auto.identity.mobile.auth.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.auth.MobileAuthMethodService;
import com.example.darks.repair_auto.identity.mobile.auth.dto.GoogleLinkRequest;
import com.example.darks.repair_auto.identity.mobile.auth.dto.MobileAuthMethodResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/me/auth-methods")
public class MobileAuthMethodController {

    private final MobileAuthMethodService service;

    public MobileAuthMethodController(MobileAuthMethodService service) {
        this.service = service;
    }

    @GetMapping
    public List<MobileAuthMethodResponse> list(@AuthenticationPrincipal AuthenticatedMobileActor actor) {
        return service.list(actor);
    }

    @PostMapping("/google")
    public ResponseEntity<Void> linkGoogle(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @Valid @RequestBody GoogleLinkRequest request) {
        service.linkGoogle(actor, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/google")
    public ResponseEntity<Void> unlinkGoogle(@AuthenticationPrincipal AuthenticatedMobileActor actor) {
        service.unlinkGoogle(actor, actor.sessionId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/telegram")
    public ResponseEntity<Void> unlinkTelegram(@AuthenticationPrincipal AuthenticatedMobileActor actor) {
        service.unlinkTelegram(actor, actor.sessionId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/phone")
    public ResponseEntity<Void> unlinkPhone(@AuthenticationPrincipal AuthenticatedMobileActor actor) {
        service.unlinkPhone(actor, actor.sessionId());
        return ResponseEntity.noContent().build();
    }
}
