package com.example.darks.repair_auto.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MobileRefreshSessionTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    @Test
    void givenValidCustomerWhenCreatedThenPropertiesSetCorrectly() {
        Customer customer = new Customer("Customer 1", "+998901234567", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(customer, "id", 100L);
        UUID familyId = UUID.randomUUID();

        MobileRefreshSession session = MobileRefreshSession.forCustomer(
                customer,
                "token-hash-123",
                familyId,
                null,
                NOW,
                NOW.plusDays(30));

        assertThat(session.getActorType()).isEqualTo(ActorType.CUSTOMER);
        assertThat(session.getCustomer()).isSameAs(customer);
        assertThat(session.getTechnician()).isNull();
        assertThat(session.getActorId()).isEqualTo(100L);
        assertThat(session.getTokenHash()).isEqualTo("token-hash-123");
        assertThat(session.getTokenFamilyId()).isEqualTo(familyId);
        assertThat(session.getParentSessionId()).isNull();
        assertThat(session.isUsed()).isFalse();
        assertThat(session.isRevoked()).isFalse();
        assertThat(session.isExpired(NOW.plusDays(10))).isFalse();
        assertThat(session.isExpired(NOW.plusDays(31))).isTrue();
    }

    @Test
    void givenValidTechnicianWhenCreatedThenPropertiesSetCorrectly() {
        Technician technician = new Technician("Tech 1", "+998909876543", "Diagnostics", "Notes", 5, LanguageCode.RU, true, NOW);
        ReflectionTestUtils.setField(technician, "id", 200L);
        UUID familyId = UUID.randomUUID();

        MobileRefreshSession session = MobileRefreshSession.forTechnician(
                technician,
                "tech-hash-456",
                familyId,
                55L,
                NOW,
                NOW.plusDays(30));

        assertThat(session.getActorType()).isEqualTo(ActorType.TECHNICIAN);
        assertThat(session.getTechnician()).isSameAs(technician);
        assertThat(session.getCustomer()).isNull();
        assertThat(session.getActorId()).isEqualTo(200L);
        assertThat(session.getParentSessionId()).isEqualTo(55L);
    }

    @Test
    void givenInvalidOwnershipCombinationsThenThrowsIllegalArgumentException() {
        Customer customer = new Customer("Customer 1", "+998901234567", LanguageCode.UZ, NOW);
        Technician technician = new Technician("Tech 1", "+998909876543", "Diagnostics", "Notes", 5, LanguageCode.RU, true, NOW);
        UUID familyId = UUID.randomUUID();

        // Customer session with null customer
        assertThatThrownBy(() -> new MobileRefreshSession(
                ActorType.CUSTOMER, null, null, "hash", familyId, null, NOW, NOW.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);

        // Customer session with technician populated
        assertThatThrownBy(() -> new MobileRefreshSession(
                ActorType.CUSTOMER, customer, technician, "hash", familyId, null, NOW, NOW.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);

        // Technician session with null technician
        assertThatThrownBy(() -> new MobileRefreshSession(
                ActorType.TECHNICIAN, null, null, "hash", familyId, null, NOW, NOW.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);

        // Technician session with customer populated
        assertThatThrownBy(() -> new MobileRefreshSession(
                ActorType.TECHNICIAN, customer, technician, "hash", familyId, null, NOW, NOW.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);

        // Unsupported actor type STAFF
        assertThatThrownBy(() -> new MobileRefreshSession(
                ActorType.STAFF, null, null, "hash", familyId, null, NOW, NOW.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);

        // Blank hash
        assertThatThrownBy(() -> MobileRefreshSession.forCustomer(
                customer, "   ", familyId, null, NOW, NOW.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);

        // Expiry before issued
        assertThatThrownBy(() -> MobileRefreshSession.forCustomer(
                customer, "hash", familyId, null, NOW, NOW.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenSessionWhenMarkUsedAndRevokedThenStateUpdated() {
        Customer customer = new Customer("Customer 1", "+998901234567", LanguageCode.UZ, NOW);
        MobileRefreshSession session = MobileRefreshSession.forCustomer(
                customer, "hash", UUID.randomUUID(), null, NOW, NOW.plusDays(30));

        session.markUsed(NOW.plusHours(1));
        assertThat(session.isUsed()).isTrue();
        assertThat(session.getLastUsedAt()).isEqualTo(NOW.plusHours(1));

        session.replaceWith(99L, NOW.plusHours(1));
        assertThat(session.getReplacedBySessionId()).isEqualTo(99L);

        session.revoke(NOW.plusHours(2), MobileRefreshRevocationReason.ROTATED.name());
        assertThat(session.isRevoked()).isTrue();
        assertThat(session.getRevokedAt()).isEqualTo(NOW.plusHours(2));
        assertThat(session.getRevocationReason()).isEqualTo("ROTATED");

        // Subsequent revoke call does not overwrite earlier revocation
        session.revoke(NOW.plusHours(3), MobileRefreshRevocationReason.LOGOUT.name());
        assertThat(session.getRevokedAt()).isEqualTo(NOW.plusHours(2));
        assertThat(session.getRevocationReason()).isEqualTo("ROTATED");
    }
}
