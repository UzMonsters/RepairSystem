package com.example.darks.repair_auto.security;

import com.example.darks.repair_auto.user.domain.User;
import com.example.darks.repair_auto.user.domain.UserRole;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String fullName;
    private final UserRole role;
    private final boolean active;
    private final OffsetDateTime passwordChangedAt;

    public AuthenticatedUser(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.active = user.isActive();
        this.passwordChangedAt = user.getPasswordChangedAt();
    }

    public Long id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String fullName() {
        return fullName;
    }

    public UserRole role() {
        return role;
    }

    public OffsetDateTime passwordChangedAt() {
        return passwordChangedAt;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
