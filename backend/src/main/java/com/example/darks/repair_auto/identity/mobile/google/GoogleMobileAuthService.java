package com.example.darks.repair_auto.identity.mobile.google;

import static com.example.darks.repair_auto.identity.mobile.auth.MobileAuthLogSupport.deviceSummary;
import static com.example.darks.repair_auto.identity.mobile.auth.MobileAuthLogSupport.present;
import static com.example.darks.repair_auto.identity.mobile.auth.MobileAuthLogSupport.safeLength;
import static com.example.darks.repair_auto.identity.mobile.auth.MobileAuthLogSupport.safeUserAgent;

import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import com.example.darks.repair_auto.identity.mobile.auth.MobileAuthenticationService;
import com.example.darks.repair_auto.identity.mobile.auth.VerifiedMobileIdentity;
import com.example.darks.repair_auto.identity.mobile.auth.dto.GoogleLoginRequest;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.MobileAuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleMobileAuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleMobileAuthService.class);

    private final GoogleIdTokenVerifier verifier;
    private final MobileAuthenticationService authenticationService;

    public GoogleMobileAuthService(
            GoogleIdTokenVerifier verifier,
            MobileAuthenticationService authenticationService) {
        this.verifier = verifier;
        this.authenticationService = authenticationService;
    }

    @Transactional
    public MobileAuthResponse login(GoogleLoginRequest request, String ip, String userAgent) {
        LOGGER.info(
                "Mobile Google login verification started clientType={} tokenPresent={} tokenLength={} "
                        + "device=[{}] ip={} userAgent={}",
                request.clientType(),
                present(request.idToken()),
                safeLength(request.idToken()),
                deviceSummary(request.device()),
                ip,
                safeUserAgent(userAgent));
        GoogleIdentity google = verifier.verify(request.idToken(), request.clientType());
        LOGGER.info(
                "Mobile Google login token verified clientType={} subjectPresent={} emailPresent={} emailVerified={}",
                request.clientType(),
                present(google.subject()),
                present(google.email()),
                google.emailVerified());
        return authenticationService.authenticate(
                request.clientType(),
                new VerifiedMobileIdentity(
                        MobileAuthProvider.GOOGLE,
                        google.subject(),
                        google.email(),
                        google.emailVerified(),
                        null,
                        google.name()),
                request.device(),
                ip,
                userAgent);
    }
}
