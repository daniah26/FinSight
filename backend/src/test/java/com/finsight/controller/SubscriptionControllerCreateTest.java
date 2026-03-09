package com.finsight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.dto.SubscriptionDto;
import com.finsight.model.Subscription;
import com.finsight.model.SubscriptionStatus;
import com.finsight.model.User;
import com.finsight.repository.SubscriptionRepository;
import com.finsight.service.SubscriptionDetectorService;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for POST /api/subscriptions (manual subscription creation)
 */
@WebMvcTest(SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SubscriptionController - POST /api/subscriptions")
class SubscriptionControllerCreateTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @MockBean private SubscriptionDetectorService detectorService;
    @MockBean private SubscriptionRepository subRepo;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("alice").email("alice@example.com").build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Valid Requests
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("200 OK — valid subscription with 30-day gap")
    void create_valid30Days_200() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(5);
        LocalDate nextDue = lastPaid.plusDays(30);
        
        Subscription saved = buildSubscription(1L, "Netflix", "15.99", lastPaid, nextDue);
        when(subRepo.save(any())).thenReturn(saved);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("Netflix", "15.99", lastPaid, nextDue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchant").value("Netflix"))
                .andExpect(jsonPath("$.avgAmount").value(15.99));
    }

    @Test
    @DisplayName("200 OK — exactly 25 days (minimum boundary)")
    void create_exactly25Days_200() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(10);
        LocalDate nextDue = lastPaid.plusDays(25);
        
        Subscription saved = buildSubscription(1L, "Spotify", "9.99", lastPaid, nextDue);
        when(subRepo.save(any())).thenReturn(saved);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("Spotify", "9.99", lastPaid, nextDue)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("200 OK — exactly 35 days (maximum boundary)")
    void create_exactly35Days_200() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(10);
        LocalDate nextDue = lastPaid.plusDays(35);
        
        Subscription saved = buildSubscription(1L, "Gym", "50.00", lastPaid, nextDue);
        when(subRepo.save(any())).thenReturn(saved);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("Gym", "50.00", lastPaid, nextDue)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("200 OK — lastPaidDate is today")
    void create_lastPaidToday_200() throws Exception {
        LocalDate lastPaid = LocalDate.now();
        LocalDate nextDue = lastPaid.plusDays(30);
        
        Subscription saved = buildSubscription(1L, "Service", "10.00", lastPaid, nextDue);
        when(subRepo.save(any())).thenReturn(saved);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("Service", "10.00", lastPaid, nextDue)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("200 OK — nextDueDate is today")
    void create_nextDueToday_200() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(30);
        LocalDate nextDue = LocalDate.now();
        
        Subscription saved = buildSubscription(1L, "Service", "10.00", lastPaid, nextDue);
        when(subRepo.save(any())).thenReturn(saved);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("Service", "10.00", lastPaid, nextDue)))
                .andExpect(status().isOk());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Date Range Validation (25-35 days)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("400 — 24 days gap (below minimum)")
    void create_24DaysGap_400() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(10);
        LocalDate nextDue = lastPaid.plusDays(24);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("Service", "10.00", lastPaid, nextDue)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Next due date must be at least 25 days after last paid date"));
    }

    @Test
    @DisplayName("400 — 36 days gap (above maximum)")
    void create_36DaysGap_400() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(10);
        LocalDate nextDue = lastPaid.plusDays(36);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("Service", "10.00", lastPaid, nextDue)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Next due date must be at most 35 days after last paid date"));
    }

    @Test
    @DisplayName("400 — nextDueDate before lastPaidDate")
    void create_nextDueBeforeLastPaid_400() throws Exception {
        LocalDate lastPaid = LocalDate.now();
        LocalDate nextDue = lastPaid.minusDays(1);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("Service", "10.00", lastPaid, nextDue)))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Field Validation
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("400 — blank merchant name")
    void create_blankMerchant_400() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(10);
        LocalDate nextDue = lastPaid.plusDays(30);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("", "10.00", lastPaid, nextDue)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(subRepo);
    }

    @Test
    @DisplayName("400 — merchant name exceeds 100 characters")
    void create_merchantTooLong_400() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(10);
        LocalDate nextDue = lastPaid.plusDays(30);
        String longName = "A".repeat(101);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson(longName, "10.00", lastPaid, nextDue)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(subRepo);
    }

    @Test
    @DisplayName("400 — amount is zero")
    void create_amountZero_400() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(10);
        LocalDate nextDue = lastPaid.plusDays(30);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("Service", "0.00", lastPaid, nextDue)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(subRepo);
    }

    @Test
    @DisplayName("400 — amount is negative")
    void create_amountNegative_400() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(10);
        LocalDate nextDue = lastPaid.plusDays(30);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("Service", "-10.00", lastPaid, nextDue)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(subRepo);
    }

    @Test
    @DisplayName("400 — amount exceeds $10,000")
    void create_amountTooHigh_400() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(10);
        LocalDate nextDue = lastPaid.plusDays(30);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("Service", "10000.01", lastPaid, nextDue)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(subRepo);
    }

    @Test
    @DisplayName("400 — lastPaidDate in the future")
    void create_lastPaidFuture_400() throws Exception {
        LocalDate lastPaid = LocalDate.now().plusDays(1);
        LocalDate nextDue = lastPaid.plusDays(30);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("Service", "10.00", lastPaid, nextDue)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(subRepo);
    }

    @Test
    @DisplayName("400 — nextDueDate in the past")
    void create_nextDuePast_400() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(60);
        LocalDate nextDue = LocalDate.now().minusDays(1);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("Service", "10.00", lastPaid, nextDue)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(subRepo);
    }

    @Test
    @DisplayName("400 — missing userId parameter")
    void create_missingUserId_400() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(10);
        LocalDate nextDue = lastPaid.plusDays(30);

        mockMvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson("Service", "10.00", lastPaid, nextDue)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 — null merchant")
    void create_nullMerchant_400() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(10);
        LocalDate nextDue = lastPaid.plusDays(30);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson(null, "10.00", lastPaid, nextDue)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 — null amount")
    void create_nullAmount_400() throws Exception {
        LocalDate lastPaid = LocalDate.now().minusDays(10);
        LocalDate nextDue = lastPaid.plusDays(30);

        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchant\":\"Service\",\"lastPaidDate\":\"" + lastPaid + "\",\"nextDueDate\":\"" + nextDue + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 — empty JSON body")
    void create_emptyBody_400() throws Exception {
        mockMvc.perform(post("/api/subscriptions")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String subscriptionJson(String merchant, String amount, LocalDate lastPaid, LocalDate nextDue) throws Exception {
        SubscriptionDto dto = SubscriptionDto.builder()
                .merchant(merchant)
                .avgAmount(amount != null ? new BigDecimal(amount) : null)
                .lastPaidDate(lastPaid)
                .nextDueDate(nextDue)
                .build();
        return mapper.writeValueAsString(dto);
    }

    private Subscription buildSubscription(Long id, String merchant, String amount, LocalDate lastPaid, LocalDate nextDue) {
        return Subscription.builder()
                .id(id)
                .user(user)
                .merchant(merchant)
                .avgAmount(new BigDecimal(amount))
                .lastPaidDate(lastPaid)
                .nextDueDate(nextDue)
                .status(SubscriptionStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
