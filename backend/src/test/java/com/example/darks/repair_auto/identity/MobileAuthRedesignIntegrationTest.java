package com.example.darks.repair_auto.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.TokenHashService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.EmailVerificationChallenge;
import com.example.darks.repair_auto.identity.domain.MobileAuthIdentity;
import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import com.example.darks.repair_auto.identity.domain.PhoneOtpChallenge;
import com.example.darks.repair_auto.identity.domain.PhoneOtpPurpose;
import com.example.darks.repair_auto.identity.infrastructure.persistence.EmailVerificationChallengeRepository;
import com.example.darks.repair_auto.identity.infrastructure.persistence.MobileAuthIdentityRepository;
import com.example.darks.repair_auto.identity.infrastructure.persistence.MobileRefreshSessionRepository;
import com.example.darks.repair_auto.identity.infrastructure.persistence.MobileSessionRepository;
import com.example.darks.repair_auto.identity.infrastructure.persistence.PhoneOtpChallengeRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.identity.mobile.google.GoogleIdTokenVerifier;
import com.example.darks.repair_auto.identity.mobile.google.GoogleIdentity;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class MobileAuthRedesignIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private MobileAuthIdentityRepository identityRepository;

    @Autowired
    private MobileSessionRepository sessionRepository;

    @Autowired
    private MobileRefreshSessionRepository refreshRepository;

    @Autowired
    private PhoneOtpChallengeRepository phoneOtpChallengeRepository;

    @Autowired
    private EmailVerificationChallengeRepository emailChallengeRepository;

    @Autowired
    private TokenHashService tokenHashService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.now(ZoneOffset.UTC);
        emailChallengeRepository.deleteAll();
        phoneOtpChallengeRepository.deleteAll();
        refreshRepository.deleteAll();
        sessionRepository.deleteAll();
        identityRepository.deleteAll();
        customerRepository.deleteAll();
        technicianRepository.deleteAll();
    }

    @Test
    void givenGoogleCustomerWhenLoginThenRegistersCustomerWithoutPhoneAndAllowsProfileAccess() throws Exception {
        Mockito.when(googleIdTokenVerifier.verify("google-token-cust-1", PushClientType.CUSTOMER_MOBILE))
                .thenReturn(new GoogleIdentity("google-sub-cust-1", "cust1@example.com", true, "Customer One"));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/mobile/auth/google/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "google-token-cust-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.actor.type").value("CUSTOMER"))
                .andExpect(jsonPath("$.actor.fullName").value("Customer One"))
                .andExpect(jsonPath("$.actor.phone").value(org.hamcrest.Matchers.nullValue()))
                .andReturn();

        String accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/v1/mobile/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actorType").value("CUSTOMER"))
                .andExpect(jsonPath("$.email").value("cust1@example.com"))
                .andExpect(jsonPath("$.emailVerified").value(true));

        mockMvc.perform(get("/api/v1/mobile/me/repair-requests")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void givenPhoneOtpCustomerWhenRequestAndVerifyThenCreatesCustomerWithPhone() throws Exception {
        String phone = "+998901112233";
        PhoneOtpChallenge challenge = new PhoneOtpChallenge(
                phone,
                ActorType.CUSTOMER,
                PushClientType.CUSTOMER_MOBILE,
                PhoneOtpPurpose.CUSTOMER_REGISTER_OR_LOGIN,
                tokenHashService.hash("123456"),
                5,
                now,
                now.plusMinutes(5),
                now.plusSeconds(60),
                "127.0.0.1",
                "TestAgent");
        challenge = phoneOtpChallengeRepository.saveAndFlush(challenge);

        MvcResult result = mockMvc.perform(post("/api/v1/mobile/auth/phone/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "challengeId": "%s",
                                  "code": "123456"
                                }
                                """.formatted(challenge.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actor.type").value("CUSTOMER"))
                .andExpect(jsonPath("$.actor.phone").value(phone))
                .andReturn();

        String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        mockMvc.perform(get("/api/v1/mobile/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value(phone))
                .andExpect(jsonPath("$.phoneVerified").value(true));
    }

    @Test
    void givenProvisionedTechnicianWhenGoogleLoginThenClaimsTechnicianAccount() throws Exception {
        Technician technician = new Technician(
                "Rustam Tech",
                "+998907778899",
                "Diagnostics",
                "Senior",
                5,
                LanguageCode.UZ,
                true,
                now);
        technician.setEmail("rustam.tech@repairauto.uz", now, now);
        technician = technicianRepository.saveAndFlush(technician);

        Mockito.when(googleIdTokenVerifier.verify("google-token-tech-1", PushClientType.TECHNICIAN_MOBILE))
                .thenReturn(new GoogleIdentity("google-sub-tech-1", "rustam.tech@repairauto.uz", true, "Rustam Tech"));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/mobile/auth/google/technician")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "google-token-tech-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actor.type").value("TECHNICIAN"))
                .andExpect(jsonPath("$.actor.id").value(technician.getId()))
                .andReturn();

        String accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
        mockMvc.perform(get("/api/v1/mobile/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actorType").value("TECHNICIAN"))
                .andExpect(jsonPath("$.email").value("rustam.tech@repairauto.uz"));
    }

    @Test
    void givenUnprovisionedTechnicianWhenGoogleLoginThenFailsWithAccountNotProvisioned() throws Exception {
        Mockito.when(googleIdTokenVerifier.verify("google-token-tech-unknown", PushClientType.TECHNICIAN_MOBILE))
                .thenReturn(new GoogleIdentity("google-sub-unknown", "unknown@repairauto.uz", true, "Unknown"));

        mockMvc.perform(post("/api/v1/mobile/auth/google/technician")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "google-token-tech-unknown"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TECHNICIAN_ACCOUNT_NOT_PROVISIONED"));

        assertThat(technicianRepository.findAll()).isEmpty();
    }

    @Test
    void givenCustomerAndTechnicianWithSameGoogleSubjectThenNamespaceIsolationPreservesBoth() throws Exception {
        Customer customer = Customer.google("Customer Shared", "shared@example.com", now, LanguageCode.UZ, now);
        customer = customerRepository.saveAndFlush(customer);
        identityRepository.saveAndFlush(MobileAuthIdentity.forCustomer(
                customer,
                MobileAuthProvider.GOOGLE,
                "shared-google-sub",
                "shared@example.com",
                null,
                now));

        Technician technician = new Technician(
                "Technician Shared",
                "+998905556677",
                "Bodywork",
                "Master",
                5,
                LanguageCode.UZ,
                true,
                now);
        technician.setEmail("shared@example.com", now, now);
        technician = technicianRepository.saveAndFlush(technician);
        identityRepository.saveAndFlush(MobileAuthIdentity.forTechnician(
                technician,
                MobileAuthProvider.GOOGLE,
                "shared-google-sub",
                "shared@example.com",
                null,
                now));

        Mockito.when(googleIdTokenVerifier.verify("shared-google-token", PushClientType.CUSTOMER_MOBILE))
                .thenReturn(new GoogleIdentity("shared-google-sub", "shared@example.com", true, "Customer Shared"));
        Mockito.when(googleIdTokenVerifier.verify("shared-google-token", PushClientType.TECHNICIAN_MOBILE))
                .thenReturn(new GoogleIdentity("shared-google-sub", "shared@example.com", true, "Technician Shared"));

        mockMvc.perform(post("/api/v1/mobile/auth/google/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "shared-google-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actor.type").value("CUSTOMER"))
                .andExpect(jsonPath("$.actor.id").value(customer.getId()));

        mockMvc.perform(post("/api/v1/mobile/auth/google/technician")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "shared-google-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actor.type").value("TECHNICIAN"))
                .andExpect(jsonPath("$.actor.id").value(technician.getId()));
    }

    @Test
    void givenActiveSessionWhenRevokedThenSubsequentRequestReturnsUnauthorized() throws Exception {
        Mockito.when(googleIdTokenVerifier.verify("google-token", PushClientType.CUSTOMER_MOBILE))
                .thenReturn(new GoogleIdentity("google-sub", "test@example.com", true, "Customer"));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/mobile/auth/google/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "google-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
        String sessionId = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.session.id");

        mockMvc.perform(get("/api/v1/mobile/me/sessions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sessionId))
                .andExpect(jsonPath("$[0].revoked").value(false));

        mockMvc.perform(delete("/api/v1/mobile/me/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/mobile/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
    }

    @Test
    void givenMultipleSessionsWhenAuthenticatedPhoneChangeThenKeepsCurrentSessionAndRevokesOthers() throws Exception {
        Mockito.when(googleIdTokenVerifier.verify("google-token", PushClientType.CUSTOMER_MOBILE))
                .thenReturn(new GoogleIdentity("google-sub-change", "change@example.com", true, "Customer Phone Change"));

        // First session (Desktop/Tablet)
        MvcResult session1Result = mockMvc.perform(post("/api/v1/mobile/auth/google/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "google-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token1 = JsonPath.read(session1Result.getResponse().getContentAsString(), "$.accessToken");

        // Second session (Phone - Current active)
        MvcResult session2Result = mockMvc.perform(post("/api/v1/mobile/auth/google/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "google-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token2 = JsonPath.read(session2Result.getResponse().getContentAsString(), "$.accessToken");

        // Verify both tokens work initially
        mockMvc.perform(get("/api/v1/mobile/me").header("Authorization", "Bearer " + token1)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/mobile/me").header("Authorization", "Bearer " + token2)).andExpect(status().isOk());

        // Perform authenticated phone change on session 2
        String newPhone = "+998909876543";
        PhoneOtpChallenge challenge = new PhoneOtpChallenge(
                newPhone,
                ActorType.CUSTOMER,
                PushClientType.CUSTOMER_MOBILE,
                PhoneOtpPurpose.CHANGE_PHONE,
                tokenHashService.hash("654321"),
                5,
                now,
                now.plusMinutes(5),
                now.plusSeconds(60),
                "127.0.0.1",
                "TestAgent");
        challenge = phoneOtpChallengeRepository.saveAndFlush(challenge);

        mockMvc.perform(post("/api/v1/mobile/me/phone/verify")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "challengeId": "%s",
                                  "code": "654321"
                                }
                                """.formatted(challenge.getId())))
                .andExpect(status().isNoContent());

        // Option A verification: token2 remains valid, token1 is revoked
        mockMvc.perform(get("/api/v1/mobile/me").header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value(newPhone))
                .andExpect(jsonPath("$.phoneVerified").value(true));

        mockMvc.perform(get("/api/v1/mobile/me").header("Authorization", "Bearer " + token1))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenCustomerWithMultipleAuthMethodsWhenUnlinkingLastAuthMethodThenThrowsConflict() throws Exception {
        Customer customer = Customer.google("Multi Auth Customer", "multiauth@example.com", now, LanguageCode.UZ, now);
        customer.setPhone("+998901239999", now, now);
        customer = customerRepository.saveAndFlush(customer);

        MobileAuthIdentity googleIdentity = MobileAuthIdentity.forCustomer(customer, MobileAuthProvider.GOOGLE, "multi-sub", "multiauth@example.com", null, now);
        MobileAuthIdentity phoneIdentity = MobileAuthIdentity.forCustomer(customer, MobileAuthProvider.PHONE, "+998901239999", null, "+998901239999", now);
        identityRepository.saveAllAndFlush(List.of(googleIdentity, phoneIdentity));

        Mockito.when(googleIdTokenVerifier.verify("multi-google-token", PushClientType.CUSTOMER_MOBILE))
                .thenReturn(new GoogleIdentity("multi-sub", "multiauth@example.com", true, "Multi Auth Customer"));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/mobile/auth/google/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "multi-google-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");

        // First unlink Google -> succeeds because Phone is still active
        mockMvc.perform(delete("/api/v1/mobile/me/auth-methods/google")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // Attempt to unlink Phone -> fails with 409 LAST_AUTH_METHOD
        mockMvc.perform(delete("/api/v1/mobile/me/auth-methods/phone")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_AUTH_METHOD"));
    }

    @Test
    void givenAuthenticatedCustomerWhenVerifyEmailThenUpdatesProfileEmailAndCanBeRemoved() throws Exception {
        Customer customer = new Customer("Email Test Customer", "+998901234444", LanguageCode.UZ, now);
        customer = customerRepository.saveAndFlush(customer);
        identityRepository.saveAndFlush(MobileAuthIdentity.forCustomer(customer, MobileAuthProvider.PHONE, "+998901234444", null, "+998901234444", now));

        PhoneOtpChallenge loginChallenge = new PhoneOtpChallenge(
                "+998901234444",
                ActorType.CUSTOMER,
                PushClientType.CUSTOMER_MOBILE,
                PhoneOtpPurpose.CUSTOMER_REGISTER_OR_LOGIN,
                tokenHashService.hash("111111"),
                5,
                now,
                now.plusMinutes(5),
                now.plusSeconds(60),
                "127.0.0.1",
                "TestAgent");
        loginChallenge = phoneOtpChallengeRepository.saveAndFlush(loginChallenge);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/mobile/auth/phone/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "challengeId": "%s",
                                  "code": "111111"
                                }
                                """.formatted(loginChallenge.getId())))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");

        EmailVerificationChallenge emailChallenge = EmailVerificationChallenge.forCustomer(
                customer,
                "verified.email@example.com",
                tokenHashService.hash("998877"),
                5,
                now,
                now.plusMinutes(10),
                now.plusSeconds(60));
        emailChallenge = emailChallengeRepository.saveAndFlush(emailChallenge);

        mockMvc.perform(post("/api/v1/mobile/me/email/verify")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "challengeId": "%s",
                                  "code": "998877"
                                }
                                """.formatted(emailChallenge.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/mobile/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("verified.email@example.com"))
                .andExpect(jsonPath("$.emailVerified").value(true));

        mockMvc.perform(delete("/api/v1/mobile/me/email")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/mobile/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(org.hamcrest.Matchers.nullValue()));
    }
}
