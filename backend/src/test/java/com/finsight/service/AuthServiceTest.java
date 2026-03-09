package com.finsight.service;

import com.finsight.dto.AuthResponse;
import com.finsight.dto.LoginRequest;
import com.finsight.dto.SignupRequest;
import com.finsight.model.User;
import com.finsight.repository.UserRepository;
import com.finsight.security.JwtUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Exhaustive unit tests for AuthService.
 *
 * STRICTNESS=LENIENT solves UnnecessaryStubbingException in
 * demo-data resilience tests where the shared stubSignupSuccess() sets up
 * demoDataService.seedUserIfEmpty but the test then overrides it.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository  userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil         jwtUtil;
    @Mock private DemoDataService demoDataService;

    @InjectMocks private AuthService authService;

    private static final String RAW  = "Secret123!";
    private static final String HASH = "$2a$10$encodedHash";
    private static final String JWT  = "header.payload.sig";

    private User savedUser;

    @BeforeEach
    void buildUser() {
        savedUser = User.builder()
                .id(1L).username("alice").email("alice@example.com")
                .password(HASH).roles(Set.of("USER")).createdAt(LocalDateTime.now())
                .build();
    }

    // ── signup() ──────────────────────────────────────────────────────────────

    @Nested @DisplayName("signup()")
    class SignupTests {

        @Test @DisplayName("Complete AuthResponse returned on success")
        void signup_success_completeResponse() {
            stubSignup();
            AuthResponse r = authService.signup(signupReq());
            assertThat(r.getUserId()).isEqualTo(1L);
            assertThat(r.getUsername()).isEqualTo("alice");
            assertThat(r.getEmail()).isEqualTo("alice@example.com");
            assertThat(r.getToken()).isEqualTo(JWT);
            assertThat(r.getMessage()).containsIgnoringCase("success");
        }

        @Test @DisplayName("Password is BCrypt-encoded before persistence")
        void signup_passwordEncoded() {
            stubSignup();
            ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
            authService.signup(signupReq());
            verify(userRepository).save(cap.capture());
            assertThat(cap.getValue().getPassword()).isEqualTo(HASH);
            assertThat(cap.getValue().getPassword()).doesNotContain(RAW);
        }

        @Test @DisplayName("encode() called exactly once with raw password")
        void signup_encodeCalledOnce() {
            stubSignup();
            authService.signup(signupReq());
            verify(passwordEncoder, times(1)).encode(RAW);
        }

        @Test @DisplayName("User is saved with USER role")
        void signup_userRoleAssigned() {
            stubSignup();
            ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
            authService.signup(signupReq());
            verify(userRepository).save(cap.capture());
            assertThat(cap.getValue().getRoles()).containsExactly("USER");
        }

        @Test @DisplayName("createdAt populated on saved user")
        void signup_createdAtSet() {
            stubSignup();
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
            authService.signup(signupReq());
            verify(userRepository).save(cap.capture());
            assertThat(cap.getValue().getCreatedAt()).isAfter(before);
        }

        @Test @DisplayName("JWT generated with persisted userId and username")
        void signup_jwtUsesPersistedIds() {
            stubSignup();
            authService.signup(signupReq());
            verify(jwtUtil).generateToken(1L, "alice");
        }

        @Test @DisplayName("demo data seeded AFTER save()")
        void signup_demoDataAfterSave() {
            stubSignup();
            authService.signup(signupReq());
            InOrder order = inOrder(userRepository, demoDataService);
            order.verify(userRepository).save(any(User.class));
            order.verify(demoDataService).seedUserIfEmpty(1L);
        }

        @Test @DisplayName("save() called exactly once")
        void signup_savesOnce() {
            stubSignup();
            authService.signup(signupReq());
            verify(userRepository, times(1)).save(any(User.class));
        }

        // ── Uniqueness guards ─────────────────────────────────────────────────

        @Test @DisplayName("Throws when username already exists")
        void signup_duplicateUsername_throws() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(savedUser));
            assertThatThrownBy(() -> authService.signup(signupReq()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Username already exists");
        }

        @Test @DisplayName("No save when username duplicate")
        void signup_duplicateUsername_noSave() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(savedUser));
            assertThatThrownBy(() -> authService.signup(signupReq()));
            verify(userRepository, never()).save(any());
        }

        @Test @DisplayName("Throws when email already registered")
        void signup_duplicateEmail_throws() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(savedUser));
            assertThatThrownBy(() -> authService.signup(signupReq()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email already exists");
        }

        @Test @DisplayName("No save when email duplicate")
        void signup_duplicateEmail_noSave() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(savedUser));
            assertThatThrownBy(() -> authService.signup(signupReq()));
            verify(userRepository, never()).save(any());
        }

        @Test @DisplayName("Username checked before email — findByEmail not called when username taken")
        void signup_usernameCheckedFirst() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(savedUser));
            assertThatThrownBy(() -> authService.signup(signupReq()))
                    .hasMessageContaining("Username already exists");
            verify(userRepository, never()).findByEmail(anyString());
        }

        // ── Demo-data resilience ──────────────────────────────────────────────

        @Test @DisplayName("Signup succeeds even when DemoDataService throws")
        void signup_demoDataThrows_signupStillSucceeds() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(RAW)).thenReturn(HASH);
            when(userRepository.save(any())).thenReturn(savedUser);
            when(jwtUtil.generateToken(1L, "alice")).thenReturn(JWT);
            when(demoDataService.seedUserIfEmpty(anyLong())).thenThrow(new RuntimeException("DB down"));
            assertThatCode(() -> authService.signup(signupReq())).doesNotThrowAnyException();
        }

        @Test @DisplayName("Token still returned when demo-data seeding fails")
        void signup_demoDataThrows_tokenStillReturned() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(RAW)).thenReturn(HASH);
            when(userRepository.save(any())).thenReturn(savedUser);
            when(jwtUtil.generateToken(1L, "alice")).thenReturn(JWT);
            when(demoDataService.seedUserIfEmpty(anyLong())).thenThrow(new RuntimeException("timeout"));
            AuthResponse r = authService.signup(signupReq());
            assertThat(r.getToken()).isEqualTo(JWT);
        }

        @Test @DisplayName("signup() never calls passwordEncoder.matches()")
        void signup_neverCallsMatches() {
            stubSignup();
            authService.signup(signupReq());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        // ── helper ────────────────────────────────────────────────────────────

        private void stubSignup() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(RAW)).thenReturn(HASH);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtUtil.generateToken(1L, "alice")).thenReturn(JWT);
            when(demoDataService.seedUserIfEmpty(1L)).thenReturn(30);
        }
    }

    // ── login() ───────────────────────────────────────────────────────────────

    @Nested @DisplayName("login()")
    class LoginTests {

        @Test @DisplayName("Complete AuthResponse returned on success")
        void login_success_completeResponse() {
            stubLogin();
            AuthResponse r = authService.login(loginReq("alice", RAW));
            assertThat(r.getUserId()).isEqualTo(1L);
            assertThat(r.getUsername()).isEqualTo("alice");
            assertThat(r.getEmail()).isEqualTo("alice@example.com");
            assertThat(r.getToken()).isEqualTo(JWT);
            assertThat(r.getMessage()).containsIgnoringCase("success");
        }

        @Test @DisplayName("JWT generated with db userId and username")
        void login_jwtUsesDbFields() {
            stubLogin();
            authService.login(loginReq("alice", RAW));
            verify(jwtUtil).generateToken(1L, "alice");
        }

        @Test @DisplayName("matches() called with raw password and stored hash")
        void login_matchesCalledCorrectly() {
            stubLogin();
            authService.login(loginReq("alice", RAW));
            verify(passwordEncoder).matches(RAW, HASH);
        }

        @Test @DisplayName("Throws for unknown username")
        void login_unknownUsername_throws() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> authService.login(loginReq("ghost", RAW)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid username or password");
        }

        @Test @DisplayName("Throws for wrong password")
        void login_wrongPassword_throws() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(savedUser));
            when(passwordEncoder.matches("wrong", HASH)).thenReturn(false);
            assertThatThrownBy(() -> authService.login(loginReq("alice", "wrong")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid username or password");
        }

        @Test @DisplayName("No token generated for unknown username")
        void login_unknownUsername_noToken() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> authService.login(loginReq("ghost", RAW)));
            verify(jwtUtil, never()).generateToken(anyLong(), anyString());
        }

        @Test @DisplayName("No token generated for wrong password")
        void login_wrongPassword_noToken() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(savedUser));
            when(passwordEncoder.matches("bad", HASH)).thenReturn(false);
            assertThatThrownBy(() -> authService.login(loginReq("alice", "bad")));
            verify(jwtUtil, never()).generateToken(anyLong(), anyString());
        }

        @Test @DisplayName("Error messages identical for unknown-user and wrong-password (no enumeration)")
        void login_errorMessages_identical() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
            String msgA = "";
            try { authService.login(loginReq("ghost", "x")); } catch (RuntimeException e) { msgA = e.getMessage(); }

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(savedUser));
            when(passwordEncoder.matches("bad", HASH)).thenReturn(false);
            String msgB = "";
            try { authService.login(loginReq("alice", "bad")); } catch (RuntimeException e) { msgB = e.getMessage(); }

            assertThat(msgA).isEqualTo(msgB);
        }

        @Test @DisplayName("login() never calls DemoDataService")
        void login_neverSeedsData() {
            stubLogin();
            authService.login(loginReq("alice", RAW));
            verifyNoInteractions(demoDataService);
        }

        @Test @DisplayName("login() never modifies any user record")
        void login_neverModifiesRepo() {
            stubLogin();
            authService.login(loginReq("alice", RAW));
            verify(userRepository, never()).save(any());
            verify(userRepository, never()).delete(any());
        }

        @Test @DisplayName("findByUsername() called exactly once")
        void login_findByUsernameOnce() {
            stubLogin();
            authService.login(loginReq("alice", RAW));
            verify(userRepository, times(1)).findByUsername("alice");
        }

        // ── helper ────────────────────────────────────────────────────────────

        private void stubLogin() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(savedUser));
            when(passwordEncoder.matches(RAW, HASH)).thenReturn(true);
            when(jwtUtil.generateToken(1L, "alice")).thenReturn(JWT);
        }
    }

    // ── request factories ─────────────────────────────────────────────────────

    private SignupRequest signupReq() {
        SignupRequest r = new SignupRequest();
        r.setUsername("alice"); r.setEmail("alice@example.com"); r.setPassword(RAW);
        return r;
    }

    private LoginRequest loginReq(String u, String p) {
        LoginRequest r = new LoginRequest();
        r.setUsername(u); r.setPassword(p);
        return r;
    }
}
