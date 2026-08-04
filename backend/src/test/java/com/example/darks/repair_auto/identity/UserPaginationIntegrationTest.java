package com.example.darks.repair_auto.identity;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.api.UserPageRequest;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class UserPaginationIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private RepairRequestRepository repairRequestRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        repairRequestRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        createUser("Admin User", "admin@example.com", "AdminPass123!", UserRole.ADMIN, true);
        createUser("Alpha Manager", "alpha@example.com", "ManagerPass123!", UserRole.MANAGER, true);
        createUser("Beta Manager", "beta@example.com", "ManagerPass123!", UserRole.MANAGER, false);
        adminToken = login("admin@example.com", "AdminPass123!");
    }

    @Test
    void givenDefaultPaginationWhenListingUsersThenMetadataUsesConfiguredDefaults() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(UserPageRequest.DEFAULT_PAGE))
                .andExpect(jsonPath("$.size").value(UserPageRequest.DEFAULT_SIZE))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void givenAllowedSortFieldsWhenListingUsersThenRequestsSucceed() throws Exception {
        for (String field : UserPageRequest.ALLOWED_SORT_FIELDS) {
            mockMvc.perform(authenticatedGet("/api/v1/users?sort=" + field + ",asc"))
                    .andExpect(status().isOk());
            mockMvc.perform(authenticatedGet("/api/v1/users?sort=" + field + ",desc"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void givenSearchRoleAndActiveFiltersWhenListingUsersThenTheyStillWork() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users?search=alpha&role=MANAGER&active=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].email").value("alpha@example.com"));
    }

    @Test
    void givenInvalidSortOrPageParametersWhenListingUsersThenStableBadRequestIsReturned() throws Exception {
        assertInvalidParameter("/api/v1/users?sort=notAField,asc", "sort", "notAField");
        assertInvalidParameter("/api/v1/users?sort=passwordHash,asc", "sort", "passwordHash");
        assertInvalidParameter("/api/v1/users?sort=authVersion,asc", "sort", "authVersion");
        assertInvalidParameter("/api/v1/users?sort=email,sideways", "sort", "direction");
        assertInvalidParameter("/api/v1/users?sort=,asc", "sort", "expression");
        assertInvalidParameter("/api/v1/users?page=-1", "page", "Page");
        assertInvalidParameter("/api/v1/users?size=0", "size", "Size");
        assertInvalidParameter("/api/v1/users?size=-1", "size", "Size");
        assertInvalidParameter("/api/v1/users?size=101", "size", "100");
    }

    private void assertInvalidParameter(String path, String field, String safeMessagePart) throws Exception {
        mockMvc.perform(authenticatedGet(path).header("X-Trace-Id", "page-sort-trace"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Trace-Id", "page-sort-trace"))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.traceId").value("page-sort-trace"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field))
                .andExpect(jsonPath("$.fieldErrors[0].message", containsString(safeMessagePart)))
                .andExpect(content().string(not(containsString("PropertyReferenceException"))))
                .andExpect(content().string(not(containsString("INTERNAL_ERROR"))));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedGet(String path) {
        return get(path).header("Authorization", "Bearer " + adminToken);
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private User createUser(String fullName, String email, String password, UserRole role, boolean active) {
        return userRepository.saveAndFlush(new User(
                fullName,
                emailNormalizer.normalize(email),
                passwordService.hash(password),
                role,
                active,
                OffsetDateTime.now(ZoneOffset.UTC)));
    }
}
