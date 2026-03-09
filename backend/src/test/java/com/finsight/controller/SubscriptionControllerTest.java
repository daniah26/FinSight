package com.finsight.controller;

//import com.finsight.dto.SubscriptionDto;
import com.finsight.model.*;
import com.finsight.repository.SubscriptionRepository;
import com.finsight.service.SubscriptionDetectorService;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for SubscriptionController.
 *
 * KEY: @AutoConfigureMockMvc(addFilters=false) disables Spring Security so
 * requests reach the controller (no 403/401 from the filter chain).
 *
 * KEY: @MockitoSettings(LENIENT) prevents UnnecessaryStubbingException.
 */
@WebMvcTest(SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SubscriptionController")
class SubscriptionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private SubscriptionDetectorService detectorService;
    @MockBean  private SubscriptionRepository      subRepo;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("alice").email("alice@example.com").build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/subscriptions
    // ══════════════════════════════════════════════════════════════════════════
    @Nested @DisplayName("GET /api/subscriptions")
    class GetSubscriptionsTests {

        @Test @DisplayName("200 OK — returns subscriptions from repository")
        void valid_200() throws Exception {
            when(subRepo.findByUserId(1L)).thenReturn(List.of(activeSub(1L, "netflix", "15.99", 3)));
            mockMvc.perform(get("/api/subscriptions").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].merchant").value("netflix"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        }

        @Test @DisplayName("200 OK — empty array when no subscriptions")
        void noSubscriptions_emptyArray() throws Exception {
            when(subRepo.findByUserId(1L)).thenReturn(List.of());
            mockMvc.perform(get("/api/subscriptions").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test @DisplayName("200 OK — multiple subscriptions returned")
        void multipleSubscriptions_allReturned() throws Exception {
            when(subRepo.findByUserId(1L)).thenReturn(List.of(
                activeSub(1L, "netflix", "15.99", 3),
                activeSub(2L, "spotify", "9.99",  5),
                ignoredSub(3L, "gym",    "50.00")
            ));
            mockMvc.perform(get("/api/subscriptions").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)));
        }

        @Test @DisplayName("SubscriptionDetectorService NOT called by GET /api/subscriptions")
        void doesNotCallDetector() throws Exception {
            when(subRepo.findByUserId(1L)).thenReturn(List.of());
            mockMvc.perform(get("/api/subscriptions").param("userId", "1"))
                    .andExpect(status().isOk());
            verifyNoInteractions(detectorService);
        }

        @Test @DisplayName("4xx error — missing userId param")
        void missingUserId_400() throws Exception {
            mockMvc.perform(get("/api/subscriptions"))
                    .andExpect(status().is4xxClientError());
        }

        @Test @DisplayName("Response DTO contains all expected fields")
        void dtoFieldsComplete() throws Exception {
            when(subRepo.findByUserId(1L)).thenReturn(List.of(activeSub(1L, "netflix", "15.99", 3)));
            mockMvc.perform(get("/api/subscriptions").param("userId", "1"))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].merchant").value("netflix"))
                    .andExpect(jsonPath("$[0].avgAmount").value(15.99))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].lastPaidDate").exists())
                    .andExpect(jsonPath("$[0].nextDueDate").exists());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/subscriptions/detect
    // ══════════════════════════════════════════════════════════════════════════
    @Nested @DisplayName("POST /api/subscriptions/detect")
    class DetectTests {

        @Test @DisplayName("200 OK — returns freshly detected subscriptions")
        void valid_200() throws Exception {
            when(detectorService.detectSubscriptions(1L))
                    .thenReturn(List.of(activeSub(1L, "netflix", "15.99", 3)));
            mockMvc.perform(post("/api/subscriptions/detect").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].merchant").value("netflix"));
        }

        @Test @DisplayName("200 OK — empty array when nothing detected")
        void noDetections_emptyArray() throws Exception {
            when(detectorService.detectSubscriptions(1L)).thenReturn(List.of());
            mockMvc.perform(post("/api/subscriptions/detect").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test @DisplayName("4xx error — missing userId param")
        void missingUserId_400() throws Exception {
            mockMvc.perform(post("/api/subscriptions/detect"))
                    .andExpect(status().is4xxClientError());
        }

        @Test @DisplayName("detectSubscriptions() called with correct userId")
        void callsServiceWithCorrectUserId() throws Exception {
            when(detectorService.detectSubscriptions(42L)).thenReturn(List.of());
            mockMvc.perform(post("/api/subscriptions/detect").param("userId", "42"))
                    .andExpect(status().isOk());
            verify(detectorService).detectSubscriptions(42L);
        }

        @Test @DisplayName("IGNORED subscription appears in response with IGNORED status")
        void ignoredInResult_statusReflected() throws Exception {
            when(detectorService.detectSubscriptions(1L))
                    .thenReturn(List.of(ignoredSub(2L, "gym", "50.00")));
            mockMvc.perform(post("/api/subscriptions/detect").param("userId", "1"))
                    .andExpect(jsonPath("$[0].status").value("IGNORED"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/subscriptions/due-soon
    // ══════════════════════════════════════════════════════════════════════════
    @Nested @DisplayName("GET /api/subscriptions/due-soon")
    class DueSoonTests {

        @Test @DisplayName("200 OK — returns subscriptions due within 7 days (default)")
        void default7Days_200() throws Exception {
            when(subRepo.findDueSoonByUserId(eq(1L), any(), any()))
                    .thenReturn(List.of(activeSub(1L, "netflix", "15.99", 2)));
            mockMvc.perform(get("/api/subscriptions/due-soon").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].merchant").value("netflix"));
        }

        @Test @DisplayName("200 OK — empty array when nothing due")
        void nothingDue_emptyArray() throws Exception {
            when(subRepo.findDueSoonByUserId(eq(1L), any(), any())).thenReturn(List.of());
            mockMvc.perform(get("/api/subscriptions/due-soon").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test @DisplayName("Custom 'days' parameter accepted")
        void customDays_accepted() throws Exception {
            when(subRepo.findDueSoonByUserId(eq(1L), any(), any())).thenReturn(List.of());
            mockMvc.perform(get("/api/subscriptions/due-soon")
                            .param("userId", "1").param("days", "14"))
                    .andExpect(status().isOk());
            verify(subRepo).findDueSoonByUserId(eq(1L), any(), any());
        }

        @Test @DisplayName("4xx error — missing userId param")
        void missingUserId_400() throws Exception {
            mockMvc.perform(get("/api/subscriptions/due-soon"))
                    .andExpect(status().is4xxClientError());
        }

        @Test @DisplayName("ACTIVE subscription returned with ACTIVE status")
        void activeReturned() throws Exception {
            when(subRepo.findDueSoonByUserId(eq(1L), any(), any()))
                    .thenReturn(List.of(activeSub(1L, "netflix", "15.99", 2)));
            mockMvc.perform(get("/api/subscriptions/due-soon").param("userId", "1"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PUT /api/subscriptions/{id}/ignore
    // ══════════════════════════════════════════════════════════════════════════
    @Nested @DisplayName("PUT /api/subscriptions/{id}/ignore")
    class IgnoreTests {

        @Test @DisplayName("200 OK — returns subscription with IGNORED status")
        void valid_200_withIgnoredStatus() throws Exception {
            Subscription sub = activeSub(1L, "netflix", "15.99", 3);
            when(subRepo.findById(1L)).thenReturn(Optional.of(sub));
            when(subRepo.save(any())).thenAnswer(inv -> {
                Subscription s = inv.getArgument(0);
                s.setStatus(SubscriptionStatus.IGNORED);
                return s;
            });
            mockMvc.perform(put("/api/subscriptions/1/ignore").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IGNORED"))
                    .andExpect(jsonPath("$.merchant").value("netflix"));
        }

        @Test @DisplayName("save() called with IGNORED status on entity")
        void setsIgnoredBeforeSave() throws Exception {
            Subscription sub = activeSub(1L, "netflix", "15.99", 3);
            when(subRepo.findById(1L)).thenReturn(Optional.of(sub));
            when(subRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            mockMvc.perform(put("/api/subscriptions/1/ignore").param("userId", "1"))
                    .andExpect(status().isOk());
            verify(subRepo).save(argThat(s -> s.getStatus() == SubscriptionStatus.IGNORED));
        }

        @Test @DisplayName("save() called exactly once")
        void savesExactlyOnce() throws Exception {
            Subscription sub = activeSub(1L, "netflix", "15.99", 3);
            when(subRepo.findById(1L)).thenReturn(Optional.of(sub));
            when(subRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            mockMvc.perform(put("/api/subscriptions/1/ignore").param("userId", "1"))
                    .andExpect(status().isOk());
            verify(subRepo, times(1)).save(any());
        }

        @Test @DisplayName("4xx error — missing userId param")
        void missingUserId_400() throws Exception {
            mockMvc.perform(put("/api/subscriptions/1/ignore"))
                    .andExpect(status().is4xxClientError());
        }

        @Test @DisplayName("Ignoring already-IGNORED subscription is idempotent — 200 OK")
        void alreadyIgnored_idempotent_200() throws Exception {
            Subscription ignored = ignoredSub(1L, "netflix", "15.99");
            when(subRepo.findById(1L)).thenReturn(Optional.of(ignored));
            when(subRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            mockMvc.perform(put("/api/subscriptions/1/ignore").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IGNORED"));
        }
    }

    // ── builder helpers ───────────────────────────────────────────────────────

    private Subscription activeSub(Long id, String merchant, String avg, int dueDays) {
        return Subscription.builder().id(id).user(user).merchant(merchant)
                .avgAmount(new BigDecimal(avg))
                .lastPaidDate(LocalDate.now().minusDays(27))
                .nextDueDate(LocalDate.now().plusDays(dueDays))
                .status(SubscriptionStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(60)).build();
    }

    private Subscription ignoredSub(Long id, String merchant, String avg) {
        return Subscription.builder().id(id).user(user).merchant(merchant)
                .avgAmount(new BigDecimal(avg))
                .lastPaidDate(LocalDate.now().minusDays(30))
                .nextDueDate(LocalDate.now())
                .status(SubscriptionStatus.IGNORED)
                .createdAt(LocalDateTime.now().minusDays(90)).build();
    }
}