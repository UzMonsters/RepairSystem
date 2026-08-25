package com.example.darks.repair_auto.shared.i18n;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AcceptLanguageLocalizationIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenInvalidCredentialsWhenAcceptLanguageUzThenReturnsUzbekMessage() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Accept-Language", "uz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unknown@example.com",
                                  "password": "wrong-password-123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Foydalanuvchi nomi yoki parol noto'g'ri."));
    }

    @Test
    void givenInvalidCredentialsWhenAcceptLanguageRuThenReturnsRussianMessage() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Accept-Language", "ru")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unknown@example.com",
                                  "password": "wrong-password-123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("\u041D\u0435\u0432\u0435\u0440\u043D\u043E\u0435 \u0438\u043C\u044F \u043F\u043E\u043B\u044C\u0437\u043E\u0432\u0430\u0442\u0435\u043B\u044F \u0438\u043B\u0438 \u043F\u0430\u0440\u043E\u043B\u044C."));
    }

    @Test
    void givenInvalidCredentialsWhenAcceptLanguageEnThenReturnsEnglishMessage() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unknown@example.com",
                                  "password": "wrong-password-123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Username or password is incorrect."));
    }

    @Test
    void givenInvalidCredentialsWhenAcceptLanguageLanguageVariantsThenResolvesCorrectly() throws Exception {
        // en-US variant
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Accept-Language", "en-US")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unknown@example.com",
                                  "password": "wrong-password-123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Username or password is incorrect."));

        // ru-RU variant
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Accept-Language", "ru-RU,ru;q=0.9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unknown@example.com",
                                  "password": "wrong-password-123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("\u041D\u0435\u0432\u0435\u0440\u043D\u043E\u0435 \u0438\u043C\u044F \u043F\u043E\u043B\u044C\u0437\u043E\u0432\u0430\u0442\u0435\u043B\u044F \u0438\u043B\u0438 \u043F\u0430\u0440\u043E\u043B\u044C."));

        // uz-UZ variant
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Accept-Language", "uz-UZ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unknown@example.com",
                                  "password": "wrong-password-123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Foydalanuvchi nomi yoki parol noto'g'ri."));
    }

    @Test
    void givenInvalidCredentialsWhenUnsupportedLanguageThenFallsBackSafelyToDefaultUzbek() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Accept-Language", "de-DE,de;q=0.9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unknown@example.com",
                                  "password": "wrong-password-123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Foydalanuvchi nomi yoki parol noto'g'ri."));
    }

    @Test
    void givenInvalidCredentialsWhenNoAcceptLanguageHeaderThenFallsBackToDefaultUzbek() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unknown@example.com",
                                  "password": "wrong-password-123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Foydalanuvchi nomi yoki parol noto'g'ri."));
    }

    @Test
    void givenUnauthenticatedRequestToProtectedRouteWhenAcceptLanguageProvidedThenReturnsLocalizedAuthError() throws Exception {
        // EN
        mockMvc.perform(get("/api/v1/repair-requests")
                        .header("Accept-Language", "en"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Authentication is required."));

        // RU
        mockMvc.perform(get("/api/v1/repair-requests")
                        .header("Accept-Language", "ru"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("\u0422\u0440\u0435\u0431\u0443\u0435\u0442\u0441\u044F \u0430\u0443\u0442\u0435\u043D\u0442\u0438\u0444\u0438\u043A\u0430\u0446\u0438\u044F."));

        // UZ
        mockMvc.perform(get("/api/v1/repair-requests")
                        .header("Accept-Language", "uz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Autentifikatsiya talab qilinadi."));
    }
}
