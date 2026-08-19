package com.example.darks.repair_auto.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.shared.error.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

class JwtTokenServiceTest {

    private static final String SECRET = "test-local-only-jwt-secret-that-is-long-enough";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void givenValidStaffTokenWhenValidatedThenClaimsAreReturned() {
        User user = user();
        JwtTokenService service = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));

        JwtTokenService.ValidatedAccessToken token = service.validate(service.issue(user));

        assertThat(token.actorType()).isEqualTo(ActorType.STAFF);
        assertThat(token.isStaff()).isTrue();
        assertThat(token.actorId()).isEqualTo(user.getId());
        assertThat(token.userId()).isEqualTo(user.getId());
        assertThat(token.subject()).isEqualTo("admin@example.com");
        assertThat(token.role()).isEqualTo(UserRole.ADMIN);
        assertThat(token.authVersion()).isEqualTo(1L);
    }

    @Test
    void givenLegacyStaffTokenWithoutActorTypeWhenValidatedThenItResolvesAsStaff() {
        JwtTokenService service = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));
        String legacyToken = legacyStaffToken(service, 99L, "legacy@example.com", UserRole.MANAGER, 2L);

        JwtTokenService.ValidatedAccessToken token = service.validate(legacyToken);

        assertThat(token.actorType()).isEqualTo(ActorType.STAFF);
        assertThat(token.isStaff()).isTrue();
        assertThat(token.userId()).isEqualTo(99L);
        assertThat(token.actorId()).isEqualTo(99L);
        assertThat(token.subject()).isEqualTo("legacy@example.com");
        assertThat(token.role()).isEqualTo(UserRole.MANAGER);
        assertThat(token.authVersion()).isEqualTo(2L);
    }

    @Test
    void givenCustomerMobileTokenWhenIssuedThenValidatesCorrectly() {
        JwtTokenService service = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));
        String tokenString = service.issueMobile(ActorType.CUSTOMER, 123L, "+998901234567");

        JwtTokenService.ValidatedAccessToken token = service.validate(tokenString);

        assertThat(token.actorType()).isEqualTo(ActorType.CUSTOMER);
        assertThat(token.isCustomer()).isTrue();
        assertThat(token.isStaff()).isFalse();
        assertThat(token.isTechnician()).isFalse();
        assertThat(token.actorId()).isEqualTo(123L);
        assertThat(token.userId()).isNull();
        assertThat(token.role()).isNull();
        assertThat(token.authVersion()).isNull();
        assertThat(token.subject()).isEqualTo("+998901234567");
    }

    @Test
    void givenTechnicianMobileTokenWhenIssuedThenValidatesCorrectly() {
        JwtTokenService service = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));
        String tokenString = service.issueMobile(ActorType.TECHNICIAN, 456L);

        JwtTokenService.ValidatedAccessToken token = service.validate(tokenString);

        assertThat(token.actorType()).isEqualTo(ActorType.TECHNICIAN);
        assertThat(token.isTechnician()).isTrue();
        assertThat(token.isCustomer()).isFalse();
        assertThat(token.isStaff()).isFalse();
        assertThat(token.actorId()).isEqualTo(456L);
        assertThat(token.userId()).isNull();
        assertThat(token.role()).isNull();
        assertThat(token.authVersion()).isNull();
        assertThat(token.subject()).isEqualTo("technician:456");
    }

    @Test
    void givenStaffActorTypePassedToIssueMobileThenThrowsIllegalArgumentException() {
        JwtTokenService service = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.issueMobile(ActorType.STAFF, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mobile access tokens can only be issued for CUSTOMER or TECHNICIAN");
    }

    @Test
    void givenNullOrNonPositiveActorIdToIssueMobileThenThrowsIllegalArgumentException() {
        JwtTokenService service = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.issueMobile(ActorType.CUSTOMER, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.issueMobile(ActorType.CUSTOMER, 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.issueMobile(ActorType.CUSTOMER, -10L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenTamperedTokenWhenValidatedThenInvalidAccessTokenIsReturned() {
        JwtTokenService service = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));
        String token = service.issue(user());
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        BusinessException exception = catchThrowableOfType(
                () -> service.validate(tampered),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("INVALID_ACCESS_TOKEN");
    }

    @Test
    void givenExpiredTokenWhenValidatedThenExpiredCodeIsReturned() {
        JwtTokenService issuer = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));
        JwtTokenService validator = service("repair-auto", Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));

        BusinessException exception = catchThrowableOfType(
                () -> validator.validate(issuer.issue(user())),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("ACCESS_TOKEN_EXPIRED");
    }

    @Test
    void givenExpiredMobileTokenWhenValidatedThenExpiredCodeIsReturned() {
        JwtTokenService issuer = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));
        JwtTokenService validator = service("repair-auto", Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));

        BusinessException exception = catchThrowableOfType(
                () -> validator.validate(issuer.issueMobile(ActorType.CUSTOMER, 12L)),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("ACCESS_TOKEN_EXPIRED");
    }

    @Test
    void givenWrongIssuerWhenValidatedThenInvalidAccessTokenIsReturned() {
        JwtTokenService issuer = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));
        JwtTokenService validator = service("other-issuer", Clock.fixed(NOW, ZoneOffset.UTC));

        BusinessException exception = catchThrowableOfType(
                () -> validator.validate(issuer.issue(user())),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("INVALID_ACCESS_TOKEN");
    }

    @Test
    void givenTokenWithoutValidAuthVersionWhenValidatedThenInvalidAccessTokenIsReturned() {
        JwtTokenService service = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));
        String missing = tokenWithAuthVersion(service, null);
        String malformed = tokenWithAuthVersion(service, "1");
        String outOfRange = tokenWithAuthVersion(service, 0);

        assertThat(catchThrowableOfType(() -> service.validate(missing), BusinessException.class).code())
                .isEqualTo("INVALID_ACCESS_TOKEN");
        assertThat(catchThrowableOfType(() -> service.validate(malformed), BusinessException.class).code())
                .isEqualTo("INVALID_ACCESS_TOKEN");
        assertThat(catchThrowableOfType(() -> service.validate(outOfRange), BusinessException.class).code())
                .isEqualTo("INVALID_ACCESS_TOKEN");
    }

    private JwtTokenService service(String issuer, Clock clock) {
        return new JwtTokenService(properties(issuer), new ObjectMapper(), clock);
    }

    private AppProperties properties(String issuer) {
        return new AppProperties(
                new AppProperties.Cors(
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        false),
                new AppProperties.Trace("X-Trace-Id"),
                new AppProperties.Jwt(SECRET, issuer, Duration.ofMinutes(15)),
                Duration.ofDays(1),
                Duration.ofDays(30),
                new AppProperties.BootstrapAdmin(false, "", "", ""));
    }

    private User user() {
        User user = new User(
                "Admin User",
                "admin@example.com",
                "$2a$12$hashedPasswordPlaceholder",
                UserRole.ADMIN,
                true,
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(user, "id", 42L);
        return user;
    }

    private String legacyStaffToken(JwtTokenService service, Long userId, String email, UserRole role, long authVersion) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", "repair-auto");
        claims.put("sub", email);
        claims.put("userId", userId);
        claims.put("role", role.name());
        claims.put("authVersion", authVersion);
        claims.put("iat", NOW.getEpochSecond());
        claims.put("exp", NOW.plus(Duration.ofMinutes(15)).getEpochSecond());
        claims.put("jti", "legacy-test-jti");
        claims.put("tokenType", "access");
        String unsigned = encode(header) + "." + encode(claims);
        return unsigned + "." + ReflectionTestUtils.invokeMethod(service, "sign", unsigned);
    }

    private String tokenWithAuthVersion(JwtTokenService service, Object authVersion) {
        String[] parts = service.issue(user()).split("\\.");
        Map<String, Object> claims = decode(parts[1]);
        if (authVersion == null) {
            claims.remove("authVersion");
        } else {
            claims.put("authVersion", authVersion);
        }
        String unsigned = parts[0] + "." + encode(claims);
        return unsigned + "." + ReflectionTestUtils.invokeMethod(service, "sign", unsigned);
    }

    private Map<String, Object> decode(String encoded) {
        try {
            return new ObjectMapper().readValue(
                    java.util.Base64.getUrlDecoder().decode(encoded),
                    new tools.jackson.core.type.TypeReference<>() {
                    });
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String encode(Map<String, Object> claims) {
        try {
            return java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(new ObjectMapper().writeValueAsBytes(claims));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
