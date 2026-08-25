package com.example.darks.repair_auto.shared.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.settings.domain.Language;
import com.example.darks.repair_auto.settings.domain.SystemSettings;
import com.example.darks.repair_auto.settings.infrastructure.SystemSettingsRepository;
import com.example.darks.repair_auto.settings.infrastructure.UserSettingsRepository;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class RequestLocaleResolverTest {

    private UserSettingsRepository userSettingsRepository;
    private SystemSettingsRepository systemSettingsRepository;
    private CustomerRepository customerRepository;
    private TechnicianRepository technicianRepository;
    private RequestLocaleResolver resolver;

    @BeforeEach
    void setUp() {
        userSettingsRepository = mock(UserSettingsRepository.class);
        systemSettingsRepository = mock(SystemSettingsRepository.class);
        customerRepository = mock(CustomerRepository.class);
        technicianRepository = mock(TechnicianRepository.class);

        resolver = new RequestLocaleResolver(
                userSettingsRepository,
                systemSettingsRepository,
                customerRepository,
                technicianRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "en, EN",
            "en-US, EN",
            "en-GB, EN",
            "ru, RU",
            "ru-RU, RU",
            "uz, UZ",
            "uz-UZ, UZ",
            "uz-Latn-UZ, UZ",
            "'ru-RU,ru;q=0.9,en;q=0.8', RU",
            "'fr, en;q=0.8', EN"
    })
    void givenAcceptLanguageVariantsThenResolvesCorrectCanonicalLanguage(String header, String expectedLang) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", header);

        SupportedLanguage language = resolver.resolveLanguage(request);

        assertThat(language).isEqualTo(SupportedLanguage.valueOf(expectedLang));
    }

    @Test
    void givenUnsupportedAcceptLanguageThenFallsBackSafelyWithoutError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "de-DE,ja;q=0.9");

        SupportedLanguage language = resolver.resolveLanguage(request);

        assertThat(language).isEqualTo(SupportedLanguage.UZ);
    }

    @Test
    void givenExplicitAcceptLanguageHeaderThenHeaderTakesPrecedenceOverSavedPreference() {
        Customer customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.UZ, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 42L);
        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        try {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8");

            SupportedLanguage language = resolver.resolveLanguage(request);

            assertThat(language).isEqualTo(SupportedLanguage.RU);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void givenNoAcceptLanguageHeaderWhenCustomerAuthenticatedThenUsesSavedCustomerPreference() {
        Customer customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.RU, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 42L);
        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        try {
            MockHttpServletRequest request = new MockHttpServletRequest();

            SupportedLanguage language = resolver.resolveLanguage(request);

            assertThat(language).isEqualTo(SupportedLanguage.RU);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void givenNoAcceptLanguageHeaderWhenTechnicianAuthenticatedThenUsesSavedTechnicianPreference() {
        Technician technician = new Technician("Aziz Karimov", "+998901112233", "Washer", "Notes", 5, LanguageCode.EN, true, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(technician, "id", 17L);
        when(technicianRepository.findById(17L)).thenReturn(Optional.of(technician));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, List.of(new SimpleGrantedAuthority("ROLE_TECHNICIAN"))));

        try {
            MockHttpServletRequest request = new MockHttpServletRequest();

            SupportedLanguage language = resolver.resolveLanguage(request);

            assertThat(language).isEqualTo(SupportedLanguage.EN);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void givenNoHeaderAndNoAuthenticatedUserThenFallsBackToSystemSettingsOrDefaultUz() {
        SystemSettings settings = mock(SystemSettings.class);
        when(settings.getDefaultLanguage()).thenReturn(Language.RU);
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        MockHttpServletRequest request = new MockHttpServletRequest();

        SupportedLanguage language = resolver.resolveLanguage(request);

        assertThat(language).isEqualTo(SupportedLanguage.RU);
    }
}
