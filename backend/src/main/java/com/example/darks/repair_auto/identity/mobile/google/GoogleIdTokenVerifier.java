package com.example.darks.repair_auto.identity.mobile.google;

import com.example.darks.repair_auto.notification.push.domain.PushClientType;
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
public class GoogleIdTokenVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleIdTokenVerifier.class);
    private static final JWSAlgorithm EXPECTED_ALGORITHM = JWSAlgorithm.RS256;

    private final GoogleOidcProperties properties;
    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final Clock clock;

    @Autowired
    public GoogleIdTokenVerifier(GoogleOidcProperties properties) {
        this(properties, createDefaultProcessor(properties.getJwksUri()), Clock.systemUTC());
    }

    GoogleIdTokenVerifier(
            GoogleOidcProperties properties,
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
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to configure Google JWKS key selector", exception);
        }
    }

    public GoogleIdentity verify(String idToken, PushClientType clientType) {
        if (!properties.isEnabled() || idToken == null || idToken.isBlank()) {
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_INVALID);
        }
        try {
            SignedJWT signedJwt = SignedJWT.parse(idToken);
            JWSHeader header = signedJwt.getHeader();
            if (!EXPECTED_ALGORITHM.equals(header.getAlgorithm())) {
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_INVALID);
            }
            JWTClaimsSet claims = jwtProcessor.process(signedJwt, null);
            if (!properties.getIssuers().contains(claims.getIssuer())) {
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_INVALID);
            }
            List<String> allowedAudiences = clientType == PushClientType.CUSTOMER_MOBILE
                    ? properties.getCustomerAllowedAudiences()
                    : properties.getTechnicianAllowedAudiences();
            if (allowedAudiences.isEmpty() || claims.getAudience().stream().noneMatch(allowedAudiences::contains)) {
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_AUDIENCE_INVALID);
            }
            Instant now = clock.instant();
            Date expiresAt = claims.getExpirationTime();
            if (expiresAt == null || expiresAt.toInstant().isBefore(now.minus(properties.getAllowedClockSkew()))) {
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_INVALID);
            }
            Date issuedAt = claims.getIssueTime();
            if (issuedAt != null && issuedAt.toInstant().isAfter(now.plus(properties.getAllowedClockSkew()))) {
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_INVALID);
            }
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_INVALID);
            }
            Boolean emailVerified = claims.getBooleanClaim("email_verified");
            return new GoogleIdentity(
                    subject,
                    claims.getStringClaim("email"),
                    Boolean.TRUE.equals(emailVerified),
                    claims.getStringClaim("name"));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            LOGGER.warn("Google ID token verification failed: {}", exception.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_INVALID);
        }
    }
}
