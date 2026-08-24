package com.example.darks.repair_auto.identity.mobile.auth.api;

import com.example.darks.repair_auto.identity.application.MobileSessionService;
import com.example.darks.repair_auto.identity.domain.MobileSessionRevocationReason;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.auth.MobileClientTypeResolver;
import com.example.darks.repair_auto.identity.mobile.auth.dto.MobileSessionResponse;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/me/sessions")
public class MobileSessionController {

    private final MobileSessionService mobileSessionService;

    public MobileSessionController(MobileSessionService mobileSessionService) {
        this.mobileSessionService = mobileSessionService;
    }

    @GetMapping
    public List<MobileSessionResponse> list(@AuthenticationPrincipal AuthenticatedMobileActor actor) {
        requireActor(actor);
        PushClientType clientType = MobileClientTypeResolver.clientType(actor.actorType());
        return mobileSessionService.list(actor.actorType(), actor.actorId(), clientType);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revoke(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable UUID sessionId) {
        requireActor(actor);
        mobileSessionService.revoke(sessionId, actor.actorType(), actor.actorId(), MobileSessionRevocationReason.ADMIN_REVOKED);
        return ResponseEntity.noContent().build();
    }

    private void requireActor(AuthenticatedMobileActor actor) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
    }
}
