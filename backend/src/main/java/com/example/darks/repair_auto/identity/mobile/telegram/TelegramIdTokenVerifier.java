package com.example.darks.repair_auto.identity.mobile.telegram;

import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.URI;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TelegramIdTokenVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramIdTokenVerifier.class);
    private static final JWSAlgorithm EXPECTED_ALGORITHM = JWSAlgorithm.RS256;

    private final TelegramLoginProperties properties;
    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final Clock clock;

    @Autowired
    public TelegramIdTokenVerifier(TelegramLoginProperties properties) {
        this(properties, createDefaultProcessor(properties.getJwksUri()), Clock.systemUTC());
    }

    public TelegramIdTokenVerifier(
            TelegramLoginProperties properties,
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor,
            Clock clock) {
        this.properties = properties;
        this.jwtProcessor = jwtProcessor;
        this.clock = clock;
    }

    private static ConfigurableJWTProcessor<SecurityContext> createDefaultProcessor(String jwksUri) {
        try {
            URL jwksUrl = URI.create(jwksUri).toURL();
            JWKSource<SecurityContext> jwkSource = JWKSourceBuilder.create(jwksUrl)
                    .cache(true)
                    .rateLimited(false)
                    .build();
            JWSKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(EXPECTED_ALGORITHM, jwkSource);
            ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            processor.setJWSKeySelector(keySelector);
            processor.setJWTClaimsSetVerifier((claimsSet, context) -> { });
            return processor;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Telegram JWKS key selector for URI: {}", jwksUri, e);
            throw new IllegalStateException("Failed to configure Telegram JWKS key selector", e);
        }
    }

    public TelegramIdentity verifyCustomerToken(String idToken) {
        String expectedClientId = properties.getCustomer().getClientId();
        if (expectedClientId == null || expectedClientId.isBlank()) {
            LOGGER.error("Customer Telegram Login Client ID is not configured.");
            throw new BusinessException(ErrorCode.TELEGRAM_AUTH_AUDIENCE_INVALID);
        }
        return verify(idToken, expectedClientId);
    }

    public TelegramIdentity verifyTechnicianToken(String idToken) {
        String expectedClientId = properties.getTechnician().getClientId();
        if (expectedClientId == null || expectedClientId.isBlank()) {
            LOGGER.error("Technician Telegram Login Client ID is not configured.");
            throw new BusinessException(ErrorCode.TELEGRAM_AUTH_AUDIENCE_INVALID);
        }
        return verify(idToken, expectedClientId);
    }

    public TelegramIdentity verify(String idToken, String expectedAudience) {
        if (idToken == null || idToken.isBlank()) {
            throw new BusinessException(ErrorCode.TELEGRAM_AUTH_INVALID);
        }
        try {
            SignedJWT signedJwt = SignedJWT.parse(idToken);
            JWSHeader header = signedJwt.getHeader();
            if (header.getAlgorithm() == null || !EXPECTED_ALGORITHM.equals(header.getAlgorithm())) {
                LOGGER.warn("Rejected Telegram ID token with unsupported algorithm: {}", header.getAlgorithm());
                throw new BusinessException(ErrorCode.TELEGRAM_AUTH_INVALID);
            }

            JWTClaimsSet claims = jwtProcessor.process(signedJwt, null);

            String expectedIssuer = properties.getIssuer();
            if (expectedIssuer == null || !expectedIssuer.equals(claims.getIssuer())) {
                LOGGER.warn("Rejected Telegram ID token with invalid issuer: expected={}, actual={}",
                        expectedIssuer, claims.getIssuer());
                throw new BusinessException(ErrorCode.TELEGRAM_AUTH_INVALID);
            }

            List<String> audience = claims.getAudience();
            if (audience == null || !audience.contains(expectedAudience)) {
                LOGGER.warn("Rejected Telegram ID token with audience mismatch: expected={}, actual={}",
                        expectedAudience, audience);
                throw new BusinessException(ErrorCode.TELEGRAM_AUTH_AUDIENCE_INVALID);
            }

            Date exp = claims.getExpirationTime();
            if (exp == null) {
                LOGGER.warn("Rejected Telegram ID token without expiration claim.");
                throw new BusinessException(ErrorCode.TELEGRAM_AUTH_INVALID);
            }
            Instant now = clock.instant();
            Instant expInstant = exp.toInstant();
            Instant earliestValidExp = now.minus(properties.getAllowedClockSkew());
            if (expInstant.isBefore(earliestValidExp)) {
                LOGGER.warn("Rejected expired Telegram ID token: exp={}, now={}", expInstant, now);
                throw new BusinessException(ErrorCode.TELEGRAM_AUTH_EXPIRED);
            }

            Date iat = claims.getIssueTime();
            if (iat != null) {
                Instant latestValidIat = now.plus(properties.getAllowedClockSkew());
                if (iat.toInstant().isAfter(latestValidIat)) {
                    LOGGER.warn("Rejected future-issued Telegram ID token: iat={}, now={}", iat.toInstant(), now);
                    throw new BusinessException(ErrorCode.TELEGRAM_AUTH_INVALID);
                }
            }

            Long telegramUserId = extractTelegramUserId(claims);
            if (telegramUserId == null || telegramUserId <= 0) {
                LOGGER.warn("Rejected Telegram ID token without valid Telegram user ID.");
                throw new BusinessException(ErrorCode.TELEGRAM_AUTH_INVALID);
            }

            String subject = claims.getSubject();
            String name = claims.getStringClaim("name");
            String username = claims.getStringClaim("preferred_username");
            if (username == null) {
                username = claims.getStringClaim("username");
            }
            String phoneNumber = claims.getStringClaim("phone_number");

            return new TelegramIdentity(telegramUserId, subject, name, username, phoneNumber);
        } catch (BusinessException e) {
            throw e;
        } catch (com.nimbusds.jose.proc.BadJOSEException e) {
            LOGGER.warn("Telegram ID token cryptographic or claims verification failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TELEGRAM_AUTH_INVALID);
        } catch (java.text.ParseException e) {
            LOGGER.warn("Malformed Telegram ID token: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TELEGRAM_AUTH_INVALID);
        } catch (Exception e) {
            LOGGER.error("Unexpected error during Telegram ID token verification", e);
            throw new BusinessException(ErrorCode.TELEGRAM_AUTH_INVALID);
        }
    }

    private Long extractTelegramUserId(JWTClaimsSet claims) {
        Object idClaim = claims.getClaim("id");
        if (idClaim instanceof Number number) {
            return number.longValue();
        }
        if (idClaim instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String sub = claims.getSubject();
        if (sub != null && !sub.isBlank()) {
            try {
                return Long.parseLong(sub.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        Object tgUserId = claims.getClaim("telegram_user_id");
        if (tgUserId instanceof Number number) {
            return number.longValue();
        }
        if (tgUserId instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
