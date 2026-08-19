package com.example.darks.repair_auto.identity.infrastructure.security;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
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

    @Autowired
    public JwtTokenService(AppProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    JwtTokenService(AppProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        this.issuer = properties.jwt().issuer();
        this.ttl = properties.jwt().accessTokenTtl();
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

    public String issueMobile(ActorType actorType, Long actorId) {
        return issueMobile(actorType, actorId, null);
    }

    public String issueMobile(ActorType actorType, Long actorId, String subject) {
        if (actorType == null || actorType == ActorType.STAFF) {
            throw new IllegalArgumentException("Mobile access tokens can only be issued for CUSTOMER or TECHNICIAN.");
        }
        if (actorId == null || actorId <= 0) {
            throw new IllegalArgumentException("actorId must be a positive number.");
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
        claims.put("actorType", actorType.name());
        claims.put("actorId", actorId);
        claims.put("role", actorType.name());
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
                            authVersion);
                }
                case CUSTOMER, TECHNICIAN -> {
                    long actorId = positiveNumberClaim(claims, "actorId");
                    yield new ValidatedAccessToken(
                            actorType,
                            actorId,
                            null,
                            subject,
                            null,
                            issuedAt,
                            null);
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

    private String stringClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof String string && !string.isBlank()) {
            return string;
        }
        throw invalid("Missing string claim.");
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
    }

    public record ValidatedAccessToken(
            ActorType actorType,
            Long actorId,
            Long userId,
            String subject,
            UserRole role,
            OffsetDateTime issuedAt,
            Long authVersion) {

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
