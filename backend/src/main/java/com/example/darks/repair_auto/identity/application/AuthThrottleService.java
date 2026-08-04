package com.example.darks.repair_auto.identity.application;

import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthThrottleService {

    private static final String LOGIN_SCOPE = "login";
    private static final String REFRESH_SCOPE = "refresh";

    private final JdbcTemplate jdbcTemplate;
    private final EmailNormalizer emailNormalizer;
    private final AuthThrottleProperties properties;
    private final Clock clock;

    public AuthThrottleService(
            JdbcTemplate jdbcTemplate,
            EmailNormalizer emailNormalizer,
            AuthThrottleProperties properties,
            Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.emailNormalizer = emailNormalizer;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public void checkLogin(String email, String clientIp) {
        check(key(LOGIN_SCOPE, emailNormalizer.normalize(email), clientIp));
    }

    @Transactional
    public void recordLoginFailure(String email, String clientIp) {
        recordFailure(key(LOGIN_SCOPE, emailNormalizer.normalize(email), clientIp));
    }

    @Transactional
    public void recordLoginSuccess(String email, String clientIp) {
        clear(key(LOGIN_SCOPE, emailNormalizer.normalize(email), clientIp));
    }

    @Transactional(readOnly = true)
    public void checkRefresh(String refreshToken, String clientIp) {
        check(key(REFRESH_SCOPE, refreshToken, clientIp));
    }

    @Transactional
    public void recordRefreshFailure(String refreshToken, String clientIp) {
        recordFailure(key(REFRESH_SCOPE, refreshToken, clientIp));
    }

    @Transactional
    public void recordRefreshSuccess(String refreshToken, String clientIp) {
        clear(key(REFRESH_SCOPE, refreshToken, clientIp));
    }

    @Transactional
    public int cleanupExpiredEntries() {
        if (!properties.enabled()) {
            return 0;
        }
        return jdbcTemplate.update("""
                delete from auth_throttle_entries
                where updated_at < ?
                    and (blocked_until is null or blocked_until < ?)
                """, now().minus(properties.retention()), now());
    }

    private void check(String throttleKey) {
        if (!properties.enabled()) {
            return;
        }
        List<OffsetDateTime> blockedUntil = jdbcTemplate.query("""
                select blocked_until
                from auth_throttle_entries
                where throttle_key = ?
                    and blocked_until is not null
                """, (rs, rowNum) -> rs.getObject("blocked_until", OffsetDateTime.class), throttleKey);
        if (!blockedUntil.isEmpty() && blockedUntil.getFirst().isAfter(now())) {
            throw new BusinessRuleException(
                    "AUTH_THROTTLED",
                    "Too many failed authentication attempts. Try again later.",
                    429);
        }
    }

    private void recordFailure(String throttleKey) {
        if (!properties.enabled()) {
            return;
        }
        OffsetDateTime now = now();
        List<Entry> rows = jdbcTemplate.query("""
                select failed_attempts, window_started_at, blocked_until
                from auth_throttle_entries
                where throttle_key = ?
                for update
                """, (rs, rowNum) -> new Entry(
                rs.getInt("failed_attempts"),
                rs.getObject("window_started_at", OffsetDateTime.class),
                rs.getObject("blocked_until", OffsetDateTime.class)), throttleKey);
        if (rows.isEmpty()) {
            jdbcTemplate.update("""
                    insert into auth_throttle_entries (
                        throttle_key, failed_attempts, window_started_at, blocked_until, updated_at
                    ) values (?, 1, ?, null, ?)
                    """, throttleKey, now, now);
            return;
        }
        Entry entry = rows.getFirst();
        boolean windowExpired = !entry.windowStartedAt().plus(properties.window()).isAfter(now);
        int failures = windowExpired ? 1 : entry.failedAttempts() + 1;
        OffsetDateTime blockedUntil = failures >= properties.maxFailures()
                ? now.plus(properties.blockDuration())
                : entry.blockedUntil();
        jdbcTemplate.update("""
                update auth_throttle_entries
                set failed_attempts = ?,
                    window_started_at = ?,
                    blocked_until = ?,
                    updated_at = ?,
                    version = version + 1
                where throttle_key = ?
                """, failures, windowExpired ? now : entry.windowStartedAt(), blockedUntil, now, throttleKey);
    }

    private void clear(String throttleKey) {
        if (!properties.enabled()) {
            return;
        }
        jdbcTemplate.update("delete from auth_throttle_entries where throttle_key = ?", throttleKey);
    }

    private String key(String scope, String credential, String clientIp) {
        return scope + ":" + sha256((clientIp == null ? "" : clientIp) + "|" + credential);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    private record Entry(int failedAttempts, OffsetDateTime windowStartedAt, OffsetDateTime blockedUntil) {
    }
}
