package com.example.darks.repair_auto.shared.i18n;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.settings.domain.Language;
import com.example.darks.repair_auto.settings.infrastructure.SystemSettingsRepository;
import com.example.darks.repair_auto.settings.infrastructure.UserSettingsRepository;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RequestLocaleResolver {

    private final UserSettingsRepository userSettingsRepository;
    private final SystemSettingsRepository systemSettingsRepository;
    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;

    public RequestLocaleResolver(
            UserSettingsRepository userSettingsRepository,
            SystemSettingsRepository systemSettingsRepository) {
        this(userSettingsRepository, systemSettingsRepository, null, null);
    }

    @Autowired
    public RequestLocaleResolver(
            UserSettingsRepository userSettingsRepository,
            SystemSettingsRepository systemSettingsRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository) {
        this.userSettingsRepository = userSettingsRepository;
        this.systemSettingsRepository = systemSettingsRepository;
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
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

        // 2. Authenticated actor's saved language (Staff User or Mobile Customer/Technician)
        SupportedLanguage fromActor = resolveFromAuthenticatedActor();
        if (fromActor != null) {
            return fromActor;
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

    private SupportedLanguage resolveFromAuthenticatedActor() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return null;
            }
            Object principal = auth.getPrincipal();
            if (principal instanceof AuthenticatedUser user) {
                if (userSettingsRepository != null) {
                    Language lang = userSettingsRepository.findByUserId(user.id())
                            .map(s -> s.getLanguage())
                            .orElse(null);
                    if (lang != null) {
                        return SupportedLanguage.fromLanguage(lang);
                    }
                }
            } else if (principal instanceof AuthenticatedMobileActor actor) {
                if (actor.isCustomer() && customerRepository != null) {
                    return customerRepository.findById(actor.actorId())
                            .map(Customer::getPreferredLanguage)
                            .map(SupportedLanguage::fromLanguageCode)
                            .orElse(null);
                } else if (actor.isTechnician() && technicianRepository != null) {
                    return technicianRepository.findById(actor.actorId())
                            .map(Technician::getPreferredLanguage)
                            .map(SupportedLanguage::fromLanguageCode)
                            .orElse(null);
                }
            }
        } catch (Exception ignored) {
            // Ignore security context lookup errors outside request threads
        }
        return null;
    }

    private SupportedLanguage resolveFromSystemSettings() {
        try {
            if (systemSettingsRepository != null) {
                Language lang = systemSettingsRepository.findById(1L)
                        .map(s -> s.getDefaultLanguage())
                        .orElse(null);
                if (lang != null) {
                    return SupportedLanguage.fromLanguage(lang);
                }
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
