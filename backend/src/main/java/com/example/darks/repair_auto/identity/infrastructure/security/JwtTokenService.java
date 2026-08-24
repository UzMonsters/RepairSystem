package com.example.darks.repair_auto.identity.infrastructure.security;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class JwtTokenService {

    private static final String TOKEN_TYPE = "access";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final byte[] secret;
    private final String issuer;
    private final Duration ttl;
    private final String mobileAudience;

    @Autowired
    public JwtTokenService(AppProperties properties, ObjectMapper objectMapper, Environment environment) {
        this(properties, objectMapper, environment, Clock.systemUTC());
    }

    public JwtTokenService(AppProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, null, Clock.systemUTC());
    }

    JwtTokenService(AppProperties properties, ObjectMapper objectMapper, Clock clock) {
        this(properties, objectMapper, null, clock);
    }

    JwtTokenService(AppProperties properties, ObjectMapper objectMapper, Environment environment, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        AppProperties.Jwt jwt = properties.jwt();
        String resolvedSecret = firstNonBlank(
                environment == null ? null : environment.getProperty("app.jwt.secret"),
                environment == null ? null : environment.getProperty("APP_JWT_SECRET"),
                jwt == null ? null : jwt.secret());
        this.secret = resolvedSecret == null ? new byte[0] : resolvedSecret.getBytes(StandardCharsets.UTF_8);
        this.issuer = firstNonBlank(
                environment == null ? null : environment.getProperty("app.jwt.issuer"),
                environment == null ? null : environment.getProperty("APP_JWT_ISSUER"),
                jwt == null ? null : jwt.issuer(),
                "repair-auto");
        this.ttl = firstNonNull(
                environment == null ? null : environment.getProperty("app.jwt.access-token-ttl", Duration.class),
                jwt == null ? null : jwt.accessTokenTtl(),
                Duration.ofMinutes(15));
        this.mobileAudience = firstNonBlank(
                environment == null ? null : environment.getProperty("app.jwt.mobile-audience"),
                environment == null ? null : environment.getProperty("APP_JWT_MOBILE_AUDIENCE"),
                jwt == null ? null : jwt.mobileAudience(),
                "repair-auto-mobile");
        validateConfiguration();
    }

    public String issue(User user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", issuer);
        claims.put("sub", user.getEmail());
        claims.put("actorType", ActorType.STAFF.name());
        claims.put("actorId", user.getId());
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("authVersion", user.getAuthVersion());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("tokenType", TOKEN_TYPE);
        String unsigned = encodeJson(header) + "." + encodeJson(claims);
        return unsigned + "." + sign(unsigned);
    }

    public String issueMobile(
            ActorType actorType,
            Long actorId,
            Long authVersion,
            UUID sessionId,
            PushClientType clientType,
            String subject) {
        if (actorType == null || actorType == ActorType.STAFF) {
            throw new IllegalArgumentException("Mobile access tokens can only be issued for CUSTOMER or TECHNICIAN.");
        }
        if (actorId == null || actorId <= 0) {
            throw new IllegalArgumentException("actorId must be a positive number.");
        }
        if (authVersion == null || authVersion < 0) {
            throw new IllegalArgumentException("authVersion must be a non-negative number.");
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId is required.");
        }
        if (clientType == null) {
            throw new IllegalArgumentException("clientType is required.");
        }
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", issuer);
        String sub = (subject != null && !subject.isBlank()) ? subject.trim() : actorType.name().toLowerCase(Locale.ROOT) + ":" + actorId;
        claims.put("sub", sub);
        claims.put("aud", mobileAudience);
        claims.put("actorType", actorType.name());
        claims.put("actorId", actorId);
        claims.put("role", actorType.name());
        claims.put("authVersion", authVersion);
        claims.put("sessionId", sessionId.toString());
        claims.put("clientType", clientType.name());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("tokenType", TOKEN_TYPE);
        String unsigned = encodeJson(header) + "." + encodeJson(claims);
        return unsigned + "." + sign(unsigned);
    }

    public ValidatedAccessToken validate(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) {
                throw invalid("Malformed access token.");
            }
            Map<String, Object> header = decodeJson(parts[0]);
            if (!"HS256".equals(header.get("alg"))) {
                throw invalid("Unsupported access token algorithm.");
            }
            String unsigned = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsigned), parts[2])) {
                throw invalid("Invalid access token signature.");
            }
            Map<String, Object> claims = decodeJson(parts[1]);
            if (!issuer.equals(claims.get("iss"))) {
                throw invalid("Invalid access token issuer.");
            }
            if (!TOKEN_TYPE.equals(claims.get("tokenType"))) {
                throw invalid("Invalid token type.");
            }
            long expiresAt = numberClaim(claims, "exp");
            if (clock.instant().getEpochSecond() >= expiresAt) {
                throw new BusinessException(ErrorCode.ACCESS_TOKEN_EXPIRED);
            }
            long iat = positiveNumberClaim(claims, "iat");
            OffsetDateTime issuedAt = OffsetDateTime.ofInstant(Instant.ofEpochSecond(iat), ZoneOffset.UTC);
            String subject = stringClaim(claims, "sub");

            ActorType actorType = resolveActorType(claims);
            return switch (actorType) {
                case STAFF -> {
                    long userId = positiveNumberClaim(claims, "userId");
                    String roleName = stringClaim(claims, "role");
                    UserRole userRole = parseUserRole(roleName);
                    long authVersion = positiveNumberClaim(claims, "authVersion");
                    yield new ValidatedAccessToken(
                            ActorType.STAFF,
                            userId,
                            userId,
                            subject,
                            userRole,
                            issuedAt,
                            authVersion,
                            null,
                            null);
                }
                case CUSTOMER, TECHNICIAN -> {
                    long actorId = positiveNumberClaim(claims, "actorId");
                    long authVersion = nonNegativeNumberClaim(claims, "authVersion");
                    UUID sessionId = uuidClaim(claims, "sessionId");
                    PushClientType clientType = clientTypeClaim(claims, "clientType");
                    String audience = stringClaim(claims, "aud");
                    if (!mobileAudience.equals(audience)) {
                        throw invalid("Invalid mobile audience.");
                    }
                    if (actorType == ActorType.CUSTOMER && clientType != PushClientType.CUSTOMER_MOBILE) {
                        throw invalid("Invalid client type for customer.");
                    }
                    if (actorType == ActorType.TECHNICIAN && clientType != PushClientType.TECHNICIAN_MOBILE) {
                        throw invalid("Invalid client type for technician.");
                    }
                    yield new ValidatedAccessToken(
                            actorType,
                            actorId,
                            null,
                            subject,
                            null,
                            issuedAt,
                            authVersion,
                            sessionId,
                            clientType);
                }
            };
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalid("Invalid access token.");
        }
    }

    private ActorType resolveActorType(Map<String, Object> claims) {
        if (claims.containsKey("actorType")) {
            String actorTypeString = stringClaim(claims, "actorType");
            try {
                return ActorType.valueOf(actorTypeString);
            } catch (IllegalArgumentException e) {
                throw invalid("Invalid actor type claim.");
            }
        }
        // Backward compatibility: If actorType claim is missing, verify legacy staff claims
        if (claims.containsKey("userId") && claims.containsKey("authVersion") && claims.containsKey("role")) {
            return ActorType.STAFF;
        }
        throw invalid("Missing or invalid actor type.");
    }

    private UserRole parseUserRole(String roleName) {
        try {
            return UserRole.valueOf(roleName);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw invalid("Invalid user role claim.");
        }
    }

    public long accessTokenTtlSeconds() {
        return ttl.toSeconds();
    }

    private void validateConfiguration() {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("APP_JWT_ISSUER must be configured.");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero() || ttl.toMinutes() > 60) {
            throw new IllegalStateException("APP_JWT_ACCESS_TOKEN_TTL must be positive and no more than 60 minutes.");
        }
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "PRODUCTION_CONFIGURATION_INVALID: APP_JWT_SECRET must contain at least 32 characters.");
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encode JWT.", exception);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            return objectMapper.readValue(bytes, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw invalid("Invalid access token payload.");
        }
    }

    private String sign(String unsigned) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign JWT.", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigestShim.equals(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private long numberClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            long longValue = number.longValue();
            if (Double.isFinite(doubleValue) && doubleValue == longValue) {
                return longValue;
            }
        }
        throw invalid("Missing numeric claim.");
    }

    private long positiveNumberClaim(Map<String, Object> claims, String name) {
        long value = numberClaim(claims, name);
        if (value > 0) {
            return value;
        }
        throw invalid("Numeric claim is outside the supported range.");
    }

    private long nonNegativeNumberClaim(Map<String, Object> claims, String name) {
        long value = numberClaim(claims, name);
        if (value >= 0) {
            return value;
        }
        throw invalid("Numeric claim is outside the supported range.");
    }

    private String stringClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof String string && !string.isBlank()) {
            return string;
        }
        throw invalid("Missing string claim.");
    }

    private Long optionalPositiveNumberClaim(Map<String, Object> claims, String name) {
        if (!claims.containsKey(name)) {
            return null;
        }
        return positiveNumberClaim(claims, name);
    }

    private Long optionalNonNegativeNumberClaim(Map<String, Object> claims, String name) {
        if (!claims.containsKey(name)) {
            return null;
        }
        return nonNegativeNumberClaim(claims, name);
    }

    private UUID uuidClaim(Map<String, Object> claims, String name) {
        try {
            return UUID.fromString(stringClaim(claims, name));
        } catch (IllegalArgumentException exception) {
            throw invalid("Invalid UUID claim.");
        }
    }

    private PushClientType clientTypeClaim(Map<String, Object> claims, String name) {
        try {
            return PushClientType.valueOf(stringClaim(claims, name));
        } catch (IllegalArgumentException exception) {
            throw invalid("Invalid client type claim.");
        }
    }

    private UUID optionalUuidClaim(Map<String, Object> claims, String name) {
        if (!claims.containsKey(name)) {
            return null;
        }
        try {
            return UUID.fromString(stringClaim(claims, name));
        } catch (IllegalArgumentException exception) {
            throw invalid("Invalid UUID claim.");
        }
    }

    private PushClientType optionalClientTypeClaim(Map<String, Object> claims, String name) {
        if (!claims.containsKey(name)) {
            return null;
        }
        try {
            return PushClientType.valueOf(stringClaim(claims, name));
        } catch (IllegalArgumentException exception) {
            throw invalid("Invalid client type claim.");
        }
    }

    private String optionalAudience(Map<String, Object> claims) {
        Object value = claims.get("aud");
        if (value instanceof String string && !string.isBlank()) {
            return string;
        }
        if (value instanceof java.util.List<?> list && !list.isEmpty() && list.get(0) instanceof String string) {
            return string;
        }
        return null;
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public record ValidatedAccessToken(
            ActorType actorType,
            Long actorId,
            Long userId,
            String subject,
            UserRole role,
            OffsetDateTime issuedAt,
            Long authVersion,
            UUID mobileSessionId,
            PushClientType clientType) {
        public ValidatedAccessToken(
                ActorType actorType,
                Long actorId,
                Long userId,
                String subject,
                UserRole role,
                OffsetDateTime issuedAt,
                Long authVersion) {
            this(actorType, actorId, userId, subject, role, issuedAt, authVersion, null, null);
        }

        public boolean isStaff() {
            return actorType == ActorType.STAFF;
        }

        public boolean isCustomer() {
            return actorType == ActorType.CUSTOMER;
        }

        public boolean isTechnician() {
            return actorType == ActorType.TECHNICIAN;
        }
    }

    private static final class MessageDigestShim {
        private static boolean equals(byte[] expected, byte[] actual) {
            return java.security.MessageDigest.isEqual(expected, actual);
        }
    }
}
