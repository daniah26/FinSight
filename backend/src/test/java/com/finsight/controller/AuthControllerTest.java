package com.finsight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.dto.AuthResponse;
import com.finsight.dto.LoginRequest;
import com.finsight.dto.SignupRequest;
import com.finsight.service.AuthService;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for AuthController — HTTP layer only.
 *
 * KEY: @AutoConfigureMockMvc(addFilters=false) disables Spring Security so
 * requests reach the controller instead of getting blocked with 403.
 *
 * KEY: @MockitoSettings(LENIENT) prevents UnnecessaryStubbingException.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired private MockMvc      mockMvc;
    @Autowired private ObjectMapper mapper;
    @MockBean  private AuthService  authService;

    private static final String TOKEN = "header.payload.sig";

    private AuthResponse signupOk() {
        return AuthResponse.builder()
                .userId(1L).username("alice").email("alice@example.com")
                .token(TOKEN).message("User registered successfully").build();
    }

    private AuthResponse loginOk() {
        return AuthResponse.builder()
                .userId(1L).username("alice").email("alice@example.com")
                .token(TOKEN).message("Login successful").build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/auth/signup
    // ══════════════════════════════════════════════════════════════════════════
    @Nested @DisplayName("POST /api/auth/signup")
    class SignupTests {

        @Test @DisplayName("201 Created + token on valid request")
        void signup_valid_201() throws Exception {
            when(authService.signup(any())).thenReturn(signupOk());
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "alice@example.com", "Secret123!")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value(TOKEN))
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.username").value("alice"))
                    .andExpect(jsonPath("$.message").value("User registered successfully"));
        }

        @Test @DisplayName("400 + message for duplicate username")
        void signup_duplicateUsername_400() throws Exception {
            when(authService.signup(any())).thenThrow(new RuntimeException("Username already exists"));
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "alice@example.com", "Secret123!")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Username already exists"));
        }

        @Test @DisplayName("400 + message for duplicate email")
        void signup_duplicateEmail_400() throws Exception {
            when(authService.signup(any())).thenThrow(new RuntimeException("Email already exists"));
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "alice@example.com", "Secret123!")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Email already exists"));
        }

        // ── Bean Validation (@Valid) ──────────────────────────────────────────

        @Test @DisplayName("400 — blank username")
        void signup_blankUsername_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("", "a@b.com", "Secret123!")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authService);
        }

        @Test @DisplayName("400 — username < 3 chars")
        void signup_usernameTooShort_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("ab", "a@b.com", "Secret123!")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authService);
        }

        @Test @DisplayName("400 — username > 20 chars")
        void signup_usernameTooLong_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("a".repeat(21), "a@b.com", "Secret123!")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authService);
        }

        @Test @DisplayName("400 — invalid email format")
        void signup_invalidEmail_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "not-an-email", "Secret123!")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authService);
        }

        @Test @DisplayName("400 — blank email")
        void signup_blankEmail_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "", "Secret123!")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authService);
        }

        @Test @DisplayName("400 — password < 8 chars")
        void signup_passwordTooShort_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "a@b.com", "Pass1!")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authService);
        }

        @Test @DisplayName("400 — blank password")
        void signup_blankPassword_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "a@b.com", "")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authService);
        }

        @Test @DisplayName("400 — empty JSON body")
        void signup_emptyBody_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Error body always has 'message' field")
        void signup_error_hasMessageField() throws Exception {
            when(authService.signup(any())).thenThrow(new RuntimeException("Username already exists"));
            mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "a@b.com", "Secret123!")))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.message").isString());
        }

        @Test @DisplayName("Error body has no 'token' field")
        void signup_error_noToken() throws Exception {
            when(authService.signup(any())).thenThrow(new RuntimeException("Username already exists"));
            mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "a@b.com", "Secret123!")))
                    .andExpect(jsonPath("$.token").doesNotExist());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/auth/login
    // ══════════════════════════════════════════════════════════════════════════
    @Nested @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test @DisplayName("200 OK + token on valid credentials")
        void login_valid_200() throws Exception {
            when(authService.login(any())).thenReturn(loginOk());
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("alice", "Secret123!")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(TOKEN))
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.message").value("Login successful"));
        }

        @Test @DisplayName("401 Unauthorized for bad credentials")
        void login_badCredentials_401() throws Exception {
            when(authService.login(any())).thenThrow(new RuntimeException("Invalid username or password"));
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("alice", "wrong")))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("401 body contains error message")
        void login_badCredentials_bodyHasMessage() throws Exception {
            when(authService.login(any())).thenThrow(new RuntimeException("Invalid username or password"));
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("alice", "wrong")))
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }

        @Test @DisplayName("400 — blank username")
        void login_blankUsername_400() throws Exception {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("", "Secret123!")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authService);
        }

        @Test @DisplayName("400 — blank password")
        void login_blankPassword_400() throws Exception {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("alice", "")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authService);
        }

        @Test @DisplayName("400 — empty JSON body")
        void login_emptyBody_400() throws Exception {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("401 body has no 'token' field")
        void login_error_noToken() throws Exception {
            when(authService.login(any())).thenThrow(new RuntimeException("Invalid username or password"));
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("alice", "bad")))
                    .andExpect(jsonPath("$.token").doesNotExist());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String signupJson(String u, String e, String p) throws Exception {
        SignupRequest r = new SignupRequest();
        r.setUsername(u); r.setEmail(e); r.setPassword(p);
        return mapper.writeValueAsString(r);
    }

    private String loginJson(String u, String p) throws Exception {
        LoginRequest r = new LoginRequest();
        r.setUsername(u); r.setPassword(p);
        return mapper.writeValueAsString(r);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Additional Security & Edge Case Tests
    // ══════════════════════════════════════════════════════════════════════════
    @Nested @DisplayName("Security & Edge Cases")
    class SecurityTests {

        @Test @DisplayName("Signup — username with special characters allowed")
        void signup_usernameWithUnderscore_201() throws Exception {
            when(authService.signup(any())).thenReturn(signupOk());
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice_123", "alice@example.com", "Secret123!")))
                    .andExpect(status().isCreated());
        }

        @Test @DisplayName("Signup — SQL injection attempt in username rejected")
        void signup_sqlInjectionUsername_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("admin'--", "test@example.com", "Secret123!")))
                    .andExpect(status().isBadRequest()); // Special characters not allowed in username
        }

        @Test @DisplayName("Signup — XSS attempt in username")
        void signup_xssUsername_201() throws Exception {
            when(authService.signup(any())).thenReturn(signupOk());
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("<script>alert('xss')</script>", "test@example.com", "Secret123!")))
                    .andExpect(status().isBadRequest()); // Too long or invalid
        }

        @Test @DisplayName("Signup — email with plus addressing")
        void signup_emailPlusAddressing_201() throws Exception {
            when(authService.signup(any())).thenReturn(signupOk());
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "alice+test@example.com", "Secret123!")))
                    .andExpect(status().isCreated());
        }

        @Test @DisplayName("Signup — email with subdomain")
        void signup_emailSubdomain_201() throws Exception {
            when(authService.signup(any())).thenReturn(signupOk());
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "alice@mail.example.com", "Secret123!")))
                    .andExpect(status().isCreated());
        }

        @Test @DisplayName("Signup — password with only numbers rejected")
        void signup_passwordOnlyNumbers_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "alice@example.com", "12345678")))
                    .andExpect(status().isBadRequest()); // Missing uppercase, lowercase, and special chars
        }

        @Test @DisplayName("Signup — very long password accepted")
        void signup_veryLongPassword_201() throws Exception {
            when(authService.signup(any())).thenReturn(signupOk());
            String longPassword = "Secret123!" + "A".repeat(90); // Valid password pattern + padding
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "alice@example.com", longPassword)))
                    .andExpect(status().isCreated());
        }

        @Test @DisplayName("Signup — null username in JSON")
        void signup_nullUsername_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":null,\"email\":\"a@b.com\",\"password\":\"Secret123!\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Signup — null email in JSON")
        void signup_nullEmail_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"email\":null,\"password\":\"Secret123!\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Signup — null password in JSON")
        void signup_nullPassword_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"email\":\"a@b.com\",\"password\":null}"))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Login — SQL injection attempt in username")
        void login_sqlInjectionUsername_401() throws Exception {
            when(authService.login(any())).thenThrow(new RuntimeException("Invalid username or password"));
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("admin' OR '1'='1", "password")))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("Login — SQL injection attempt in password")
        void login_sqlInjectionPassword_401() throws Exception {
            when(authService.login(any())).thenThrow(new RuntimeException("Invalid username or password"));
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("alice", "' OR '1'='1")))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("Login — null username in JSON")
        void login_nullUsername_400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":null,\"password\":\"Secret123!\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Login — null password in JSON")
        void login_nullPassword_400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":null}"))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Login — whitespace-only username")
        void login_whitespaceUsername_400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("   ", "Secret123!")))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Login — whitespace-only password")
        void login_whitespacePassword_400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("alice", "   ")))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Signup — whitespace-only username")
        void signup_whitespaceUsername_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("   ", "a@b.com", "Secret123!")))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Signup — whitespace-only email")
        void signup_whitespaceEmail_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "   ", "Secret123!")))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Signup — whitespace-only password")
        void signup_whitespacePassword_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "a@b.com", "   ")))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Signup — email without domain")
        void signup_emailNoDomain_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "alice@", "Secret123!")))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Signup — email without @ symbol")
        void signup_emailNoAt_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "aliceexample.com", "Secret123!")))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Signup — username exactly 3 chars (minimum boundary)")
        void signup_username3Chars_201() throws Exception {
            when(authService.signup(any())).thenReturn(signupOk());
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("abc", "a@b.com", "Secret123!")))
                    .andExpect(status().isCreated());
        }

        @Test @DisplayName("Signup — username exactly 20 chars (maximum boundary)")
        void signup_username20Chars_201() throws Exception {
            when(authService.signup(any())).thenReturn(signupOk());
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("a".repeat(20), "a@b.com", "Secret123!")))
                    .andExpect(status().isCreated());
        }

        @Test @DisplayName("Signup — password exactly 6 chars (minimum boundary)")
        void signup_password8Chars_201() throws Exception {
            when(authService.signup(any())).thenReturn(signupOk());
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "a@b.com", "Pass123!")))
                    .andExpect(status().isCreated());
        }

        @Test @DisplayName("Signup — password missing uppercase letter")
        void signup_passwordNoUppercase_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "a@b.com", "pass123!")))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Signup — password missing lowercase letter")
        void signup_passwordNoLowercase_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "a@b.com", "PASS123!")))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Signup — password missing digit")
        void signup_passwordNoDigit_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "a@b.com", "Password!")))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Signup — password missing special character")
        void signup_passwordNoSpecial_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "a@b.com", "Password123")))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Signup — username with invalid special characters")
        void signup_usernameInvalidChars_400() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice-bob", "a@b.com", "Secret123!")))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Signup — password too long (>128 chars)")
        void signup_passwordTooLong_400() throws Exception {
            String longPassword = "Secret123!" + "A".repeat(120); // 130 chars total
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupJson("alice", "a@b.com", longPassword)))
                    .andExpect(status().isBadRequest());
        }
    }
}
