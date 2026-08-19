package com.example.darks.repair_auto.identity.mobile.profile.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.profile.api.dto.MobileProfilePatchRequest;
import com.example.darks.repair_auto.identity.mobile.profile.api.dto.MobileProfileResponse;
import com.example.darks.repair_auto.identity.mobile.profile.application.MobileProfileService;
import com.example.darks.repair_auto.shared.error.ApiErrorResponseFactory;
import com.example.darks.repair_auto.shared.error.GlobalExceptionHandler;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.shared.observability.TraceIdService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MobileProfileControllerTest {

    private MockMvc mockMvc;
    private MobileProfileService mobileProfileService;
    private LocalizationService localizationService;

    @BeforeEach
    void setUp() {
        mobileProfileService = mock(MobileProfileService.class);
        localizationService = mock(LocalizationService.class);
        TraceIdService traceIdService = new TraceIdService();
        ApiErrorResponseFactory errorResponseFactory = new ApiErrorResponseFactory(traceIdService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(localizationService, errorResponseFactory);

        MobileProfileController controller = new MobileProfileController(mobileProfileService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        when(localizationService.get(any())).thenReturn("Localized message");
    }

    @Test
    void givenCustomerPrincipalWhenGetProfileThenReturns200WithCustomerData() throws Exception {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                actor,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            MobileProfileResponse response = MobileProfileResponse.forCustomer(
                    42L,
                    "Ali Valiyev",
                    "+998901234567",
                    "uz",
                    true);

            when(mobileProfileService.getProfile(actor)).thenReturn(response);

            mockMvc.perform(get("/api/v1/mobile/me").principal(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.actorType").value("CUSTOMER"))
                    .andExpect(jsonPath("$.id").value(42))
                    .andExpect(jsonPath("$.fullName").value("Ali Valiyev"))
                    .andExpect(jsonPath("$.phone").value("+998901234567"))
                    .andExpect(jsonPath("$.preferredLanguage").value("uz"))
                    .andExpect(jsonPath("$.telegramLinked").value(true))
                    .andExpect(jsonPath("$.technician").doesNotExist());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void givenTechnicianPrincipalWhenGetProfileThenReturns200WithTechnicianData() throws Exception {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                actor,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TECHNICIAN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            MobileProfileResponse response = MobileProfileResponse.forTechnician(
                    17L,
                    "Aziz Karimov",
                    "+998901112233",
                    "ru",
                    true,
                    "Washer",
                    5,
                    true);

            when(mobileProfileService.getProfile(actor)).thenReturn(response);

            mockMvc.perform(get("/api/v1/mobile/me").principal(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.actorType").value("TECHNICIAN"))
                    .andExpect(jsonPath("$.id").value(17))
                    .andExpect(jsonPath("$.fullName").value("Aziz Karimov"))
                    .andExpect(jsonPath("$.phone").value("+998901112233"))
                    .andExpect(jsonPath("$.preferredLanguage").value("ru"))
                    .andExpect(jsonPath("$.telegramLinked").value(true))
                    .andExpect(jsonPath("$.technician.specialization").value("Washer"))
                    .andExpect(jsonPath("$.technician.maxActiveJobs").value(5))
                    .andExpect(jsonPath("$.technician.active").value(true));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void givenCustomerPrincipalWhenPatchProfileThenReturns200() throws Exception {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                actor,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            MobileProfileResponse response = MobileProfileResponse.forCustomer(
                    42L,
                    "New Name",
                    "+998901234567",
                    "ru",
                    true);

            when(mobileProfileService.updateProfile(eq(actor), any(MobileProfilePatchRequest.class))).thenReturn(response);

            mockMvc.perform(patch("/api/v1/mobile/me")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "fullName": "New Name",
                                      "preferredLanguage": "ru"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("New Name"))
                    .andExpect(jsonPath("$.preferredLanguage").value("ru"));

            verify(mobileProfileService).updateProfile(eq(actor), any(MobileProfilePatchRequest.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void givenOversizedFullNameWhenPatchProfileThenReturns400BadRequest() throws Exception {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                actor,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            String longName = "A".repeat(161);
            mockMvc.perform(patch("/api/v1/mobile/me")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "fullName": "%s"
                                    }
                                    """.formatted(longName)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
