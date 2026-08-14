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
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class EffectiveLanguageResolver {

    private final HttpServletRequest request;
    private final UserSettingsRepository userSettingsRepository;
    private final SystemSettingsRepository systemSettingsRepository;

    public EffectiveLanguageResolver(
            HttpServletRequest request,
            UserSettingsRepository userSettingsRepository,
            SystemSettingsRepository systemSettingsRepository) {
        this.request = request;
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
        return null;
    }

    private Language resolveFromAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return userSettingsRepository.findByUserId(user.id())
                    .map(s -> s.getLanguage())
                    .orElse(null);
        }
        return null;
    }

    private Language resolveFromSystemSettings() {
        return systemSettingsRepository.findById(1L)
                .map(s -> s.getDefaultLanguage())
                .orElse(null);
    }
}
