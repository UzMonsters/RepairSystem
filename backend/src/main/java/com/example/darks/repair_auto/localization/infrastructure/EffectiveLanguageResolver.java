package com.example.darks.repair_auto.localization.infrastructure;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.settings.domain.Language;
import com.example.darks.repair_auto.settings.infrastructure.SystemSettingsRepository;
import com.example.darks.repair_auto.settings.infrastructure.UserSettingsRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class EffectiveLanguageResolver {

    private final UserSettingsRepository userSettingsRepository;
    private final SystemSettingsRepository systemSettingsRepository;

    public EffectiveLanguageResolver(
            UserSettingsRepository userSettingsRepository,
            SystemSettingsRepository systemSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
        this.systemSettingsRepository = systemSettingsRepository;
    }

    public Language resolveEffectiveLanguage() {
        // 1. Accept-Language header
        Language fromHeader = resolveFromAcceptLanguageHeader();
        if (fromHeader != null) {
            return fromHeader;
        }

        // 2. Authenticated user's saved language
        Language fromUser = resolveFromAuthenticatedUser();
        if (fromUser != null) {
            return fromUser;
        }

        // 3. System default language
        Language fromSystem = resolveFromSystemSettings();
        if (fromSystem != null) {
            return fromSystem;
        }

        // 4. Fallback default UZ
        return Language.UZ;
    }

    private Language resolveFromAcceptLanguageHeader() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        try {
            Enumeration<String> headers = request.getHeaders("Accept-Language");
            if (headers == null) {
                return null;
            }
            while (headers.hasMoreElements()) {
                String headerValue = headers.nextElement();
                if (headerValue != null && !headerValue.isBlank()) {
                    // Split by comma for multi-language headers like "ru, uz;q=0.9, en;q=0.8"
                    String[] parts = headerValue.split(",");
                    for (String part : parts) {
                        String code = part.split(";")[0].trim();
                        Language lang = Language.fromString(code);
                        if (lang != null) {
                            return lang;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Gracefully ignore header parsing issues
        }
        return null;
    }

    private Language resolveFromAuthenticatedUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
                return userSettingsRepository.findByUserId(user.id())
                        .map(s -> s.getLanguage())
                        .orElse(null);
            }
        } catch (Exception ignored) {
            // Ignore security context lookup errors outside request threads
        }
        return null;
    }

    private Language resolveFromSystemSettings() {
        try {
            return systemSettingsRepository.findById(1L)
                    .map(s -> s.getDefaultLanguage())
                    .orElse(null);
        } catch (Exception ignored) {
            // Fall back cleanly if DB unavailable during early startup/health checks
        }
        return null;
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
