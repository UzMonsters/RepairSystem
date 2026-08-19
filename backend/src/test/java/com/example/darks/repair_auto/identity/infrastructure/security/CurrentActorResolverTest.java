package com.example.darks.repair_auto.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.shared.error.BusinessException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class CurrentActorResolverTest {

    private final CurrentActorResolver resolver = new CurrentActorResolver();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenNoAuthenticationWhenResolvingThenReturnsEmpty() {
        assertThat(resolver.currentPrincipal()).isEmpty();
        assertThat(resolver.currentStaff()).isEmpty();
        assertThat(resolver.currentMobileActor()).isEmpty();
        assertThat(resolver.currentActorType()).isEmpty();
        assertThat(resolver.currentActorId()).isEmpty();
    }

    @Test
    void givenStaffAuthenticationWhenResolvingThenStaffIsReturned() {
        User user = new User("Staff Admin", "admin@test.com", "hash", UserRole.ADMIN, true, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(user, "id", 10L);
        AuthenticatedUser staff = new AuthenticatedUser(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(staff, "token", staff.getAuthorities()));

        assertThat(resolver.currentStaff()).contains(staff);
        assertThat(resolver.requireStaff()).isEqualTo(staff);
        assertThat(resolver.currentActorType()).contains(ActorType.STAFF);
        assertThat(resolver.currentActorId()).contains(10L);
        assertThat(resolver.currentMobileActor()).isEmpty();

        assertThatThrownBy(resolver::requireMobileActor)
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(resolver::requireCustomer)
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(resolver::requireTechnician)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void givenCustomerAuthenticationWhenResolvingThenCustomerIsReturned() {
        AuthenticatedMobileActor customerActor = new AuthenticatedMobileActor(
                ActorType.CUSTOMER,
                25L,
                "+998901234567",
                true);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(customerActor, "token", customerActor.getAuthorities()));

        assertThat(resolver.currentMobileActor()).contains(customerActor);
        assertThat(resolver.requireMobileActor()).isEqualTo(customerActor);
        assertThat(resolver.requireCustomer()).isEqualTo(customerActor);
        assertThat(resolver.currentActorType()).contains(ActorType.CUSTOMER);
        assertThat(resolver.currentActorId()).contains(25L);
        assertThat(resolver.currentStaff()).isEmpty();

        assertThatThrownBy(resolver::requireStaff)
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(resolver::requireTechnician)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void givenTechnicianAuthenticationWhenResolvingThenTechnicianIsReturned() {
        AuthenticatedMobileActor techActor = new AuthenticatedMobileActor(
                ActorType.TECHNICIAN,
                50L,
                "+998909876543",
                true);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(techActor, "token", techActor.getAuthorities()));

        assertThat(resolver.currentMobileActor()).contains(techActor);
        assertThat(resolver.requireMobileActor()).isEqualTo(techActor);
        assertThat(resolver.requireTechnician()).isEqualTo(techActor);
        assertThat(resolver.currentActorType()).contains(ActorType.TECHNICIAN);
        assertThat(resolver.currentActorId()).contains(50L);
        assertThat(resolver.currentStaff()).isEmpty();

        assertThatThrownBy(resolver::requireStaff)
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(resolver::requireCustomer)
                .isInstanceOf(BusinessException.class);
    }
}
