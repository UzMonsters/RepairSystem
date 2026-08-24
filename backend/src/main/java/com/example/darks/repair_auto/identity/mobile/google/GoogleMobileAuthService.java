package com.example.darks.repair_auto.identity.mobile.google;

import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import com.example.darks.repair_auto.identity.mobile.auth.MobileAuthenticationService;
import com.example.darks.repair_auto.identity.mobile.auth.VerifiedMobileIdentity;
import com.example.darks.repair_auto.identity.mobile.auth.dto.GoogleLoginRequest;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.MobileAuthResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleMobileAuthService {

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
        GoogleIdentity google = verifier.verify(request.idToken(), request.clientType());
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
