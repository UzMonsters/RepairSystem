package com.example.darks.repair_auto.identity.mobile.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TelegramIdTokenVerifierTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String ISSUER = "https://oauth.telegram.org";
    private static final String CUSTOMER_CLIENT_ID = "cust-client-123";
    private static final String TECHNICIAN_CLIENT_ID = "tech-client-456";

    private RSAKey rsaKey;
    private TelegramLoginProperties properties;
    private TelegramIdTokenVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        rsaKey = new RSAKeyGenerator(2048)
                .keyID("tg-test-key-1")
                .generate();

        properties = new TelegramLoginProperties();
        properties.setIssuer(ISSUER);
        properties.getCustomer().setClientId(CUSTOMER_CLIENT_ID);
        properties.getTechnician().setClientId(TECHNICIAN_CLIENT_ID);
        properties.setAllowedClockSkew(Duration.ofSeconds(60));

        ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey.toPublicJWK()));
        JWSKeySelector<SecurityContext> keySelector =
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
        ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(keySelector);
        processor.setJWTClaimsSetVerifier((claimsSet, context) -> { });

        verifier = new TelegramIdTokenVerifier(properties, processor, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void givenValidCustomerTelegramTokenWhenVerifiedThenIdentityIsReturned() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(CUSTOMER_CLIENT_ID)
                .subject("987654321")
                .claim("id", 987654321L)
                .claim("name", "John Doe")
                .claim("preferred_username", "johndoe")
                .claim("phone_number", "+998901234567")
                .claim("phone_number_verified", true)
                .issueTime(Date.from(NOW.minusSeconds(10)))
                .expirationTime(Date.from(NOW.plusSeconds(300)))
                .build();

        String token = sign(claims, rsaKey);

        TelegramIdentity identity = verifier.verifyCustomerToken(token);

        assertThat(identity.telegramUserId()).isEqualTo(987654321L);
        assertThat(identity.subject()).isEqualTo("987654321");
        assertThat(identity.name()).isEqualTo("John Doe");
        assertThat(identity.username()).isEqualTo("johndoe");
        assertThat(identity.phoneNumber()).isEqualTo("+998901234567");
        assertThat(identity.phoneNumberVerified()).isTrue();
    }

    @Test
    void givenValidTechnicianTelegramTokenWhenVerifiedThenIdentityIsReturned() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(TECHNICIAN_CLIENT_ID)
                .subject("123456789")
                .claim("id", 123456789L)
                .claim("name", "Tech Master")
                .claim("username", "techmaster")
                .issueTime(Date.from(NOW.minusSeconds(10)))
                .expirationTime(Date.from(NOW.plusSeconds(300)))
                .build();

        String token = sign(claims, rsaKey);

        TelegramIdentity identity = verifier.verifyTechnicianToken(token);

        assertThat(identity.telegramUserId()).isEqualTo(123456789L);
        assertThat(identity.name()).isEqualTo("Tech Master");
        assertThat(identity.username()).isEqualTo("techmaster");
    }

    @Test
    void givenInvalidSignatureWhenVerifiedThenTelegramAuthInvalidExceptionThrown() throws Exception {
        RSAKey differentKey = new RSAKeyGenerator(2048)
                .keyID("untrusted-key")
                .generate();

        JWTClaimsSet claims = validCustomerClaims().build();
        String token = sign(claims, differentKey);

        BusinessException exception = catchThrowableOfType(
                () -> verifier.verifyCustomerToken(token),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TELEGRAM_AUTH_INVALID);
    }

    @Test
    void givenWrongIssuerWhenVerifiedThenTelegramAuthInvalidExceptionThrown() throws Exception {
        JWTClaimsSet claims = validCustomerClaims()
                .issuer("https://untrusted.issuer.com")
                .build();

        String token = sign(claims, rsaKey);

        BusinessException exception = catchThrowableOfType(
                () -> verifier.verifyCustomerToken(token),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TELEGRAM_AUTH_INVALID);
    }

    @Test
    void givenWrongAudienceWhenVerifiedCustomerThenAudienceInvalidExceptionThrown() throws Exception {
        JWTClaimsSet claims = validCustomerClaims()
                .audience(TECHNICIAN_CLIENT_ID)
                .build();

        String token = sign(claims, rsaKey);

        BusinessException exception = catchThrowableOfType(
                () -> verifier.verifyCustomerToken(token),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TELEGRAM_AUTH_AUDIENCE_INVALID);
    }

    @Test
    void givenWrongAudienceWhenVerifiedTechnicianThenAudienceInvalidExceptionThrown() throws Exception {
        JWTClaimsSet claims = validCustomerClaims()
                .audience(CUSTOMER_CLIENT_ID)
                .build();

        String token = sign(claims, rsaKey);

        BusinessException exception = catchThrowableOfType(
                () -> verifier.verifyTechnicianToken(token),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TELEGRAM_AUTH_AUDIENCE_INVALID);
    }

    @Test
    void givenExpiredTokenWhenVerifiedThenTelegramAuthExpiredExceptionThrown() throws Exception {
        JWTClaimsSet claims = validCustomerClaims()
                .issueTime(Date.from(NOW.minusSeconds(700)))
                .expirationTime(Date.from(NOW.minusSeconds(100)))
                .build();

        String token = sign(claims, rsaKey);

        BusinessException exception = catchThrowableOfType(
                () -> verifier.verifyCustomerToken(token),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TELEGRAM_AUTH_EXPIRED);
    }

    @Test
    void givenFutureIssuedTokenBeyondClockSkewWhenVerifiedThenTelegramAuthInvalidExceptionThrown() throws Exception {
        JWTClaimsSet claims = validCustomerClaims()
                .issueTime(Date.from(NOW.plusSeconds(300)))
                .expirationTime(Date.from(NOW.plusSeconds(900)))
                .build();

        String token = sign(claims, rsaKey);

        BusinessException exception = catchThrowableOfType(
                () -> verifier.verifyCustomerToken(token),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TELEGRAM_AUTH_INVALID);
    }

    @Test
    void givenMissingTelegramUserIdClaimWhenVerifiedThenTelegramAuthInvalidExceptionThrown() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(CUSTOMER_CLIENT_ID)
                .subject("not-a-number")
                .issueTime(Date.from(NOW.minusSeconds(10)))
                .expirationTime(Date.from(NOW.plusSeconds(300)))
                .build();

        String token = sign(claims, rsaKey);

        BusinessException exception = catchThrowableOfType(
                () -> verifier.verifyCustomerToken(token),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TELEGRAM_AUTH_INVALID);
    }

    @Test
    void givenNullOrBlankTokenWhenVerifiedThenTelegramAuthInvalidExceptionThrown() {
        assertThat(catchThrowableOfType(() -> verifier.verifyCustomerToken(null), BusinessException.class).getErrorCode())
                .isEqualTo(ErrorCode.TELEGRAM_AUTH_INVALID);
        assertThat(catchThrowableOfType(() -> verifier.verifyCustomerToken(""), BusinessException.class).getErrorCode())
                .isEqualTo(ErrorCode.TELEGRAM_AUTH_INVALID);
        assertThat(catchThrowableOfType(() -> verifier.verifyCustomerToken("   "), BusinessException.class).getErrorCode())
                .isEqualTo(ErrorCode.TELEGRAM_AUTH_INVALID);
    }

    @Test
    void givenMalformedJwtWhenVerifiedThenTelegramAuthInvalidExceptionThrown() {
        BusinessException exception = catchThrowableOfType(
                () -> verifier.verifyCustomerToken("malformed.jwt.token"),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TELEGRAM_AUTH_INVALID);
    }

    @Test
    void givenUnconfiguredCustomerClientIdWhenVerifiedThenAudienceInvalidExceptionThrown() {
        properties.getCustomer().setClientId("");

        BusinessException exception = catchThrowableOfType(
                () -> verifier.verifyCustomerToken("any-token"),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TELEGRAM_AUTH_AUDIENCE_INVALID);
    }

    @Test
    void givenUnconfiguredTechnicianClientIdWhenVerifiedThenAudienceInvalidExceptionThrown() {
        properties.getTechnician().setClientId("");

        BusinessException exception = catchThrowableOfType(
                () -> verifier.verifyTechnicianToken("any-token"),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TELEGRAM_AUTH_AUDIENCE_INVALID);
    }

    @Test
    void givenNumericSubjectClaimWhenNoIdClaimThenExtractsTelegramUserIdFromSubject() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(CUSTOMER_CLIENT_ID)
                .subject("5544332211")
                .issueTime(Date.from(NOW.minusSeconds(10)))
                .expirationTime(Date.from(NOW.plusSeconds(300)))
                .build();

        String token = sign(claims, rsaKey);

        TelegramIdentity identity = verifier.verifyCustomerToken(token);

        assertThat(identity.telegramUserId()).isEqualTo(5544332211L);
    }

    private JWTClaimsSet.Builder validCustomerClaims() {
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(List.of(CUSTOMER_CLIENT_ID))
                .subject("987654321")
                .claim("id", 987654321L)
                .claim("name", "John Doe")
                .issueTime(Date.from(NOW.minusSeconds(10)))
                .expirationTime(Date.from(NOW.plusSeconds(300)));
    }

    private String sign(JWTClaimsSet claims, RSAKey key) throws Exception {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(key.getKeyID())
                .build();
        SignedJWT signedJwt = new SignedJWT(header, claims);
        signedJwt.sign(new RSASSASigner(key));
        return signedJwt.serialize();
    }
}
