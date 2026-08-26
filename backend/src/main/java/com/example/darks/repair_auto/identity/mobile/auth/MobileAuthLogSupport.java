package com.example.darks.repair_auto.identity.mobile.auth;

import com.example.darks.repair_auto.identity.mobile.auth.dto.MobileDeviceContextRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.http.HttpHeaders;

public final class MobileAuthLogSupport {

    private static final int MAX_USER_AGENT_LENGTH = 160;
    private static final int MAX_SAFE_VALUE_LENGTH = 80;

    private MobileAuthLogSupport() {
    }

    public static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    public static String safeUserAgent(HttpServletRequest request) {
        return safeUserAgent(request.getHeader(HttpHeaders.USER_AGENT));
    }

    public static String safeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }
        String cleaned = userAgent.replaceAll("[\\r\\n\\t]", " ").trim();
        if (cleaned.length() <= MAX_USER_AGENT_LENGTH) {
            return cleaned;
        }
        return cleaned.substring(0, MAX_USER_AGENT_LENGTH) + "...";
    }

    public static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    public static int safeLength(String value) {
        return value == null ? 0 : value.length();
    }

    public static String deviceSummary(MobileDeviceContextRequest device) {
        if (device == null) {
            return "none";
        }
        return "platform=" + device.platform()
                + ", appVersion=" + safeValue(device.appVersion())
                + ", deviceIdHash=" + fingerprint(device.deviceId())
                + ", deviceNamePresent=" + present(device.deviceName());
    }

    public static String fingerprint(String value) {
        if (!present(value)) {
            return "none";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 6);
        } catch (NoSuchAlgorithmException exception) {
            return "unavailable";
        }
    }

    private static String safeValue(String value) {
        if (!present(value)) {
            return "unknown";
        }
        String cleaned = value.replaceAll("[\\r\\n\\t]", " ").trim();
        if (cleaned.length() <= MAX_SAFE_VALUE_LENGTH) {
            return cleaned;
        }
        return cleaned.substring(0, MAX_SAFE_VALUE_LENGTH) + "...";
    }
}
