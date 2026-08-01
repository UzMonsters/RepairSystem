package com.example.darks.repair_auto.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.example.darks.repair_auto.common.error.BusinessRuleException;
import com.example.darks.repair_auto.config.AppProperties;
import com.example.darks.repair_auto.user.domain.User;
import com.example.darks.repair_auto.user.domain.UserRole;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

class JwtTokenServiceTest {

    private static final String SECRET = "test-local-only-jwt-secret-that-is-long-enough";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void givenValidTokenWhenValidatedThenClaimsAreReturned() {
        User user = user();
        JwtTokenService service = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));

        JwtTokenService.ValidatedAccessToken token = service.validate(service.issue(user));

        assertThat(token.userId()).isEqualTo(user.getId());
        assertThat(token.subject()).isEqualTo("admin@example.com");
        assertThat(token.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void givenTamperedTokenWhenValidatedThenInvalidAccessTokenIsReturned() {
        JwtTokenService service = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));
        String token = service.issue(user());
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        BusinessRuleException exception = catchThrowableOfType(
                () -> service.validate(tampered),
                BusinessRuleException.class);

        assertThat(exception.code()).isEqualTo("INVALID_ACCESS_TOKEN");
    }

    @Test
    void givenExpiredTokenWhenValidatedThenExpiredCodeIsReturned() {
        JwtTokenService issuer = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));
        JwtTokenService validator = service("repair-auto", Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));

        BusinessRuleException exception = catchThrowableOfType(
                () -> validator.validate(issuer.issue(user())),
                BusinessRuleException.class);

        assertThat(exception.code()).isEqualTo("ACCESS_TOKEN_EXPIRED");
    }

    @Test
    void givenWrongIssuerWhenValidatedThenInvalidAccessTokenIsReturned() {
        JwtTokenService issuer = service("repair-auto", Clock.fixed(NOW, ZoneOffset.UTC));
        JwtTokenService validator = service("other-issuer", Clock.fixed(NOW, ZoneOffset.UTC));

        BusinessRuleException exception = catchThrowableOfType(
                () -> validator.validate(issuer.issue(user())),
                BusinessRuleException.class);

        assertThat(exception.code()).isEqualTo("INVALID_ACCESS_TOKEN");
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
}
