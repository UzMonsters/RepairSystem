package com.example.darks.repair_auto.shared.i18n;

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
public class RequestLocaleResolver {

    private final UserSettingsRepository userSettingsRepository;
    private final SystemSettingsRepository systemSettingsRepository;

    public RequestLocaleResolver(
            UserSettingsRepository userSettingsRepository,
            SystemSettingsRepository systemSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
        this.systemSettingsRepository = systemSettingsRepository;
    }

    public SupportedLanguage resolveLanguage() {
        return resolveLanguage(getCurrentRequest());
    }

    public SupportedLanguage resolveLanguage(HttpServletRequest request) {
        // 1. Explicit Accept-Language request header
        SupportedLanguage fromHeader = resolveFromAcceptLanguageHeader(request);
        if (fromHeader != null) {
            return fromHeader;
        }

        // 2. Authenticated user's saved language
        SupportedLanguage fromUser = resolveFromAuthenticatedUser();
        if (fromUser != null) {
            return fromUser;
        }

        // 3. System default language
        SupportedLanguage fromSystem = resolveFromSystemSettings();
        if (fromSystem != null) {
            return fromSystem;
        }

        // 4. Default Uzbek (UZ)
        return SupportedLanguage.UZ;
    }

    private SupportedLanguage resolveFromAcceptLanguageHeader(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Enumeration<String> headers = request.getHeaders("Accept-Language");
        if (headers == null) {
            return null;
        }
        while (headers.hasMoreElements()) {
            String headerValue = headers.nextElement();
            if (headerValue != null && !headerValue.isBlank()) {
                String[] parts = headerValue.split(",");
                for (String part : parts) {
                    String code = part.split(";")[0].trim();
                    SupportedLanguage lang = SupportedLanguage.fromCode(code);
                    if (lang != null) {
                        return lang;
                    }
                }
            }
        }
        return null;
    }

    private SupportedLanguage resolveFromAuthenticatedUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
                Language lang = userSettingsRepository.findByUserId(user.id())
                        .map(s -> s.getLanguage())
                        .orElse(null);
                if (lang != null) {
                    return SupportedLanguage.fromLanguage(lang);
                }
            }
        } catch (Exception ignored) {
            // Ignore security context lookup errors outside request threads
        }
        return null;
    }

    private SupportedLanguage resolveFromSystemSettings() {
        try {
            Language lang = systemSettingsRepository.findById(1L)
                    .map(s -> s.getDefaultLanguage())
                    .orElse(null);
            if (lang != null) {
                return SupportedLanguage.fromLanguage(lang);
            }
        } catch (Exception ignored) {
            // Fall back cleanly if DB unavailable during early startup/health checks
        }
        return null;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
