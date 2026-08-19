package com.example.darks.repair_auto.identity.infrastructure.security;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentActorResolver {

    public Optional<Object> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        return Optional.ofNullable(principal);
    }

    public Optional<AuthenticatedUser> currentStaff() {
        return currentPrincipal()
                .filter(AuthenticatedUser.class::isInstance)
                .map(AuthenticatedUser.class::cast);
    }

    public Optional<AuthenticatedMobileActor> currentMobileActor() {
        return currentPrincipal()
                .filter(AuthenticatedMobileActor.class::isInstance)
                .map(AuthenticatedMobileActor.class::cast);
    }

    public Optional<ActorType> currentActorType() {
        return currentPrincipal().flatMap(principal -> {
            if (principal instanceof AuthenticatedUser) {
                return Optional.of(ActorType.STAFF);
            }
            if (principal instanceof AuthenticatedMobileActor mobile) {
                return Optional.of(mobile.actorType());
            }
            return Optional.empty();
        });
    }

    public Optional<Long> currentActorId() {
        return currentPrincipal().flatMap(principal -> {
            if (principal instanceof AuthenticatedUser user) {
                return Optional.of(user.id());
            }
            if (principal instanceof AuthenticatedMobileActor mobile) {
                return Optional.of(mobile.actorId());
            }
            return Optional.empty();
        });
    }

    public AuthenticatedUser requireStaff() {
        return currentStaff().orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
    }

    public AuthenticatedMobileActor requireMobileActor() {
        return currentMobileActor().orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
    }

    public AuthenticatedMobileActor requireCustomer() {
        return currentMobileActor()
                .filter(AuthenticatedMobileActor::isCustomer)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
    }

    public AuthenticatedMobileActor requireTechnician() {
        return currentMobileActor()
                .filter(AuthenticatedMobileActor::isTechnician)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
    }
}
