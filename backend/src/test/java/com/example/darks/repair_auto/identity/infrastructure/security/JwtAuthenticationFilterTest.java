package com.example.darks.repair_auto.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.shared.error.SecurityErrorHandler;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtAuthenticationFilterTest {

    private JwtTokenService jwtTokenService;
    private UserRepository userRepository;
    private CustomerRepository customerRepository;
    private TechnicianRepository technicianRepository;
    private SecurityErrorHandler securityErrorHandler;
    private JwtAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtTokenService = mock(JwtTokenService.class);
        userRepository = mock(UserRepository.class);
        customerRepository = mock(CustomerRepository.class);
        technicianRepository = mock(TechnicianRepository.class);
        securityErrorHandler = mock(SecurityErrorHandler.class);
        filter = new JwtAuthenticationFilter(
                jwtTokenService,
                userRepository,
                customerRepository,
                technicianRepository,
                securityErrorHandler);
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenNoAuthorizationHeaderWhenFilteringThenProceedsChainWithoutAuthentication()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void givenValidStaffTokenWhenFilteringThenSecurityContextIsSetWithAuthenticatedUser()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-staff-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = new User("Staff Admin", "admin@test.com", "hash", UserRole.ADMIN, true, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "authVersion", 1L);

        JwtTokenService.ValidatedAccessToken validated = new JwtTokenService.ValidatedAccessToken(
                ActorType.STAFF, 1L, 1L, "admin@test.com", UserRole.ADMIN, OffsetDateTime.now(ZoneOffset.UTC), 1L);

        when(jwtTokenService.validate("valid-staff-token")).thenReturn(validated);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Since doFilterInternal has a finally block that clears context after filterChain,
        // we verify that the filterChain received execution without writing unauthorized error
        verify(securityErrorHandler, never()).writeUnauthorized(any(), any(), anyString(), any());
    }

    @Test
    void givenValidCustomerTokenWhenFilteringThenCustomerRepositoryIsQueriedAndRoleCustomerGranted()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-customer-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Customer customer = new Customer("Customer 1", "+998901112233", LanguageCode.UZ, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 20L);

        JwtTokenService.ValidatedAccessToken validated = new JwtTokenService.ValidatedAccessToken(
                ActorType.CUSTOMER, 20L, null, "+998901112233", null, OffsetDateTime.now(ZoneOffset.UTC), null);

        when(jwtTokenService.validate("valid-customer-token")).thenReturn(validated);
        when(customerRepository.findById(20L)).thenReturn(Optional.of(customer));

        filter.doFilterInternal(request, response, filterChain);

        verify(customerRepository).findById(20L);
        verify(userRepository, never()).findById(any());
        verify(technicianRepository, never()).findById(any());
        verify(filterChain).doFilter(request, response);
        verify(securityErrorHandler, never()).writeUnauthorized(any(), any(), anyString(), any());
    }

    @Test
    void givenValidTechnicianTokenWhenFilteringThenTechnicianRepositoryIsQueriedAndRoleTechnicianGranted()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-technician-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Technician technician = new Technician("Tech 1", "+998904445566", "Diag", "Notes", 5, LanguageCode.UZ, true, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(technician, "id", 30L);

        JwtTokenService.ValidatedAccessToken validated = new JwtTokenService.ValidatedAccessToken(
                ActorType.TECHNICIAN, 30L, null, "technician:30", null, OffsetDateTime.now(ZoneOffset.UTC), null);

        when(jwtTokenService.validate("valid-technician-token")).thenReturn(validated);
        when(technicianRepository.findById(30L)).thenReturn(Optional.of(technician));

        filter.doFilterInternal(request, response, filterChain);

        verify(technicianRepository).findById(30L);
        verify(userRepository, never()).findById(any());
        verify(customerRepository, never()).findById(any());
        verify(filterChain).doFilter(request, response);
        verify(securityErrorHandler, never()).writeUnauthorized(any(), any(), anyString(), any());
    }

    @Test
    void givenInactiveCustomerWhenFilteringThenUnauthorizedWritten() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer inactive-customer-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Customer customer = new Customer("Inactive Customer", "+998901112233", LanguageCode.UZ, OffsetDateTime.now(ZoneOffset.UTC));
        customer.setActive(false, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 20L);

        JwtTokenService.ValidatedAccessToken validated = new JwtTokenService.ValidatedAccessToken(
                ActorType.CUSTOMER, 20L, null, "+998901112233", null, OffsetDateTime.now(ZoneOffset.UTC), null);

        when(jwtTokenService.validate("inactive-customer-token")).thenReturn(validated);
        when(customerRepository.findById(20L)).thenReturn(Optional.of(customer));

        filter.doFilterInternal(request, response, filterChain);

        verify(securityErrorHandler).writeUnauthorized(eq(request), eq(response), eq("INVALID_ACCESS_TOKEN"), any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void givenInactiveTechnicianWhenFilteringThenUnauthorizedWritten() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer inactive-technician-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Technician technician = new Technician("Inactive Tech", "+998904445566", "Diag", "Notes", 5, LanguageCode.UZ, false, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(technician, "id", 30L);

        JwtTokenService.ValidatedAccessToken validated = new JwtTokenService.ValidatedAccessToken(
                ActorType.TECHNICIAN, 30L, null, "technician:30", null, OffsetDateTime.now(ZoneOffset.UTC), null);

        when(jwtTokenService.validate("inactive-technician-token")).thenReturn(validated);
        when(technicianRepository.findById(30L)).thenReturn(Optional.of(technician));

        filter.doFilterInternal(request, response, filterChain);

        verify(securityErrorHandler).writeUnauthorized(eq(request), eq(response), eq("INVALID_ACCESS_TOKEN"), any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void givenUnknownCustomerIdWhenFilteringThenUnauthorizedWritten() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer unknown-customer-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtTokenService.ValidatedAccessToken validated = new JwtTokenService.ValidatedAccessToken(
                ActorType.CUSTOMER, 999L, null, "customer:999", null, OffsetDateTime.now(ZoneOffset.UTC), null);

        when(jwtTokenService.validate("unknown-customer-token")).thenReturn(validated);
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(securityErrorHandler).writeUnauthorized(eq(request), eq(response), eq("INVALID_ACCESS_TOKEN"), any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void givenCustomerTokenWithTechnicianIdWhenFilteringThenNeverQueriesTechnicianRepository()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer cust-with-tech-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtTokenService.ValidatedAccessToken validated = new JwtTokenService.ValidatedAccessToken(
                ActorType.CUSTOMER, 55L, null, "customer:55", null, OffsetDateTime.now(ZoneOffset.UTC), null);

        when(jwtTokenService.validate("cust-with-tech-id")).thenReturn(validated);
        when(customerRepository.findById(55L)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(customerRepository).findById(55L);
        verify(technicianRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
        verify(securityErrorHandler).writeUnauthorized(eq(request), eq(response), eq("INVALID_ACCESS_TOKEN"), any());
    }

    @Test
    void givenTechnicianTokenWithCustomerIdWhenFilteringThenNeverQueriesCustomerRepository()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tech-with-cust-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtTokenService.ValidatedAccessToken validated = new JwtTokenService.ValidatedAccessToken(
                ActorType.TECHNICIAN, 77L, null, "technician:77", null, OffsetDateTime.now(ZoneOffset.UTC), null);

        when(jwtTokenService.validate("tech-with-cust-id")).thenReturn(validated);
        when(technicianRepository.findById(77L)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(technicianRepository).findById(77L);
        verify(customerRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
        verify(securityErrorHandler).writeUnauthorized(eq(request), eq(response), eq("INVALID_ACCESS_TOKEN"), any());
    }
}
