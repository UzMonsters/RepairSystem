package com.example.darks.repair_auto.identity.infrastructure.security;

import com.example.darks.repair_auto.identity.domain.ActorType;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AuthenticatedMobileActor(
        ActorType actorType,
        Long actorId,
        String identifier,
        boolean active
) implements UserDetails {

    public AuthenticatedMobileActor {
        Objects.requireNonNull(actorType, "actorType must not be null");
        if (actorType == ActorType.STAFF) {
            throw new IllegalArgumentException("Mobile actor principal must be CUSTOMER or TECHNICIAN.");
        }
        if (actorId == null || actorId <= 0) {
            throw new IllegalArgumentException("actorId must be a positive number.");
        }
    }

    public AuthenticatedMobileActor(ActorType actorType, Long actorId) {
        this(actorType, actorId, actorType.name().toLowerCase() + ":" + actorId, true);
    }

    public AuthenticatedMobileActor(ActorType actorType, Long actorId, String identifier) {
        this(actorType, actorId, identifier, true);
    }

    public boolean isCustomer() {
        return actorType == ActorType.CUSTOMER;
    }

    public boolean isTechnician() {
        return actorType == ActorType.TECHNICIAN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + actorType.name()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return identifier != null ? identifier : actorType.name().toLowerCase() + ":" + actorId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
