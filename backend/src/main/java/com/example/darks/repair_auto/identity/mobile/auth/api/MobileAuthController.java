package com.example.darks.repair_auto.identity.mobile.auth.api;

import com.example.darks.repair_auto.identity.mobile.auth.dto.GoogleLoginRequest;
import com.example.darks.repair_auto.identity.mobile.auth.dto.MobileDeviceContextRequest;
import com.example.darks.repair_auto.identity.mobile.auth.dto.PhoneOtpRequest;
import com.example.darks.repair_auto.identity.mobile.auth.dto.PhoneOtpResponse;
import com.example.darks.repair_auto.identity.mobile.auth.dto.PhoneOtpVerifyRequest;
import com.example.darks.repair_auto.identity.mobile.google.GoogleMobileAuthService;
import com.example.darks.repair_auto.identity.mobile.otp.PhoneOtpService;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.MobileAuthResponse;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/auth")
public class MobileAuthController {

    private final GoogleMobileAuthService googleMobileAuthService;
    private final PhoneOtpService phoneOtpService;

    public MobileAuthController(
            GoogleMobileAuthService googleMobileAuthService,
            PhoneOtpService phoneOtpService) {
        this.googleMobileAuthService = googleMobileAuthService;
        this.phoneOtpService = phoneOtpService;
    }

    public record GoogleIdTokenPayload(
            @NotBlank String idToken,
            @Valid MobileDeviceContextRequest device
    ) {
    }

    @PostMapping("/google")
    public MobileAuthResponse google(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest) {
        return googleMobileAuthService.login(request, httpRequest.getRemoteAddr(), userAgent(httpRequest));
    }

    @PostMapping("/google/customer")
    public MobileAuthResponse googleCustomer(
            @Valid @RequestBody GoogleIdTokenPayload request,
            HttpServletRequest httpRequest) {
        return googleMobileAuthService.login(
                new GoogleLoginRequest(PushClientType.CUSTOMER_MOBILE, request.idToken(), request.device()),
                httpRequest.getRemoteAddr(),
                userAgent(httpRequest));
    }

    @PostMapping("/google/technician")
    public MobileAuthResponse googleTechnician(
            @Valid @RequestBody GoogleIdTokenPayload request,
            HttpServletRequest httpRequest) {
        return googleMobileAuthService.login(
                new GoogleLoginRequest(PushClientType.TECHNICIAN_MOBILE, request.idToken(), request.device()),
                httpRequest.getRemoteAddr(),
                userAgent(httpRequest));
    }

    @PostMapping({"/phone/request-otp", "/phone/otp/request", "/otp/send"})
    public PhoneOtpResponse requestPhoneOtp(
            @Valid @RequestBody PhoneOtpRequest request,
            HttpServletRequest httpRequest) {
        return phoneOtpService.request(request, httpRequest.getRemoteAddr(), userAgent(httpRequest));
    }

    @PostMapping({"/phone/verify-otp", "/phone/otp/verify", "/otp/verify"})
    public MobileAuthResponse verifyPhoneOtp(
            @Valid @RequestBody PhoneOtpVerifyRequest request,
            HttpServletRequest httpRequest) {
        return phoneOtpService.verify(request, httpRequest.getRemoteAddr(), userAgent(httpRequest));
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
