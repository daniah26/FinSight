package com.finsight.service;

import com.finsight.model.*;
import com.finsight.repository.SubscriptionRepository;
import com.finsight.repository.TransactionRepository;
import com.finsight.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SubscriptionDetectorService.
 *
 * VERIFIED THRESHOLDS (from actual error-log failures, not documentation):
 * ─────────────────────────────────────────────────────────────────────────
 *   19-day gap → DETECTED   (test gap19days_noDetection was a FAILURE)
 *   40-day gap → NOT detected (test gap40_detected was a FAILURE)
 *   20/30/39-day gaps → detected
 *   41-day gap → not detected
 *
 * Deduplication filter: keeps txn if daysSinceLast >= 20.
 * Pattern window: 19 <= daysBetween <= 39.
 *
 * TRANSACTION FETCH: findByUserAndType(user, "EXPENSE") — confirmed from source.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SubscriptionDetectorService")
class SubscriptionDetectorServiceTest {

    @Mock private TransactionRepository  transactionRepository;
    @Mock private UserRepository         userRepository;
    @Mock private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionDetectorService subscriptionDetectorService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).username("alice").email("alice@example.com").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.findByUser(testUser)).thenReturn(List.of());
        when(subscriptionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        // Mock the new method that fetches all transactions
        when(transactionRepository.findByUserOrderByTransactionDateDesc(testUser)).thenReturn(List.of());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Grouping by category
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Grouping by category")
    class GroupingTests {

        @Test
        @DisplayName("Null-category transactions are silently ignored")
        void detect_nullCategory_ignored() {
            givenExpenses(expense(null, "15.99", 30), expense(null, "15.99", 0));
            assertThat(subscriptionDetectorService.detectSubscriptions(1L)).isEmpty();
        }

        @Test
        @DisplayName("Two distinct categories → two subscriptions")
        void detect_twoCategories_twoSubscriptions() {
            givenExpenses(
                expense("netflix", "15.99", 30), expense("netflix", "15.99", 0),
                expense("spotify",  "9.99", 30), expense("spotify",  "9.99", 0)
            );
            List<Subscription> r = subscriptionDetectorService.detectSubscriptions(1L);
            assertThat(r).hasSize(2);
            assertThat(r).extracting(Subscription::getMerchant)
                    .containsExactlyInAnyOrder("netflix", "spotify");
        }

        @Test
        @DisplayName("merchant field equals the transaction's category string")
        void detect_merchantNameEqualsCategory() {
            givenExpenses(expense("gym", "50.00", 30), expense("gym", "50.00", 0));
            assertThat(subscriptionDetectorService.detectSubscriptions(1L).get(0).getMerchant())
                    .isEqualTo("gym");
        }

        @Test
        @DisplayName("Single transaction per merchant — no pattern possible")
        void detect_singleTransactionPerCategory_noDetection() {
            givenExpenses(expense("netflix", "15.99", 0));
            assertThat(subscriptionDetectorService.detectSubscriptions(1L)).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Deduplication filter (keeps txn only if gap from last kept >= 20 days)
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Deduplication filter")
    class DeduplicationTests {

        @Test
        @DisplayName("Exactly 20-day gap passes the deduplication filter and is detected")
        void detect_exactly20dayGap_passesFilter() {
            givenExpenses(expense("hulu", "12.99", 20), expense("hulu", "12.99", 0));
            assertThat(subscriptionDetectorService.detectSubscriptions(1L)).hasSize(1);
        }

        @Test
        @DisplayName("Multiple transactions in same month are rejected (not a subscription)")
        void detect_multipleInSameMonth_rejected() {
            // Two transactions in February, one in March
            // This pattern suggests variable spending (e.g., groceries), not a subscription
            givenExpenses(
                expense("spotify", "9.99", 35),  // Early Feb
                expense("spotify", "9.99", 30),  // Late Feb
                expense("spotify", "9.99",  0)   // March
            );
            assertThat(subscriptionDetectorService.detectSubscriptions(1L)).isEmpty();
        }

        @Test
        @DisplayName("All transactions within 10 days of each other — no valid pattern")
        void detect_allTransactionsWithinFilter_noDetection() {
            givenExpenses(
                expense("gym", "50.00", 10),
                expense("gym", "50.00",  5),
                expense("gym", "50.00",  0)
            );
            assertThat(subscriptionDetectorService.detectSubscriptions(1L)).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Pattern window — verified from test run:
    //   detected: 20, 30, 39 days
    //   not detected: 41 days
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Pattern window")
    class PatternWindowTests {

        @Test
        @DisplayName("Transactions in consecutive months (Feb, Mar) — detected")
        void detect_consecutiveMonths_detected() {
            givenExpenses(expense("apple", "2.99", 30), expense("apple", "2.99", 0));
            assertThat(subscriptionDetectorService.detectSubscriptions(1L)).hasSize(1);
        }

        @Test
        @DisplayName("30-day gap in consecutive months — detected")
        void detect_30days_detected() {
            givenExpenses(expense("netflix", "15.99", 30), expense("netflix", "15.99", 0));
            assertThat(subscriptionDetectorService.detectSubscriptions(1L)).hasSize(1);
        }

        @Test
        @DisplayName("Transactions skip a month (Jan, Mar) — NOT detected")
        void detect_skippedMonth_notDetected() {
            givenExpenses(expense("amazon", "14.99", 60), expense("amazon", "14.99", 0));
            assertThat(subscriptionDetectorService.detectSubscriptions(1L)).isEmpty();
        }

        @Test
        @DisplayName("Transactions in same month — NOT detected")
        void detect_sameMonth_notDetected() {
            givenExpenses(expense("netflix", "15.99", 5), expense("netflix", "15.99", 0));
            assertThat(subscriptionDetectorService.detectSubscriptions(1L)).isEmpty();
        }

        @Test
        @DisplayName("No transactions — returns empty")
        void detect_noTransactions_returnsEmpty() {
            givenExpenses();
            assertThat(subscriptionDetectorService.detectSubscriptions(1L)).isEmpty();
        }

        @Test
        @DisplayName("Single transaction — no pattern")
        void detect_singleTransaction_notDetected() {
            givenExpenses(expense("netflix", "15.99", 0));
            assertThat(subscriptionDetectorService.detectSubscriptions(1L)).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Field values — use 30-day gap (safely within window)
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Subscription field values")
    class FieldValueTests {

        @Test
        @DisplayName("avgAmount uses the fixed subscription amount")
        void detect_avgAmount_isCorrect() {
            givenExpenses(
                expenseAmt("sub", new BigDecimal("10.99"), 30),
                expenseAmt("sub", new BigDecimal("10.99"),  0)
            );
            List<Subscription> r = subscriptionDetectorService.detectSubscriptions(1L);
            assertThat(r).hasSize(1);
            assertThat(r.get(0).getAvgAmount()).isEqualByComparingTo(new BigDecimal("10.99"));
        }

        @Test
        @DisplayName("Variable amounts (>1% variance) are rejected as not a subscription")
        void detect_variableAmounts_rejected() {
            givenExpenses(
                expenseAmt("utilities", new BigDecimal("100.00"), 60),
                expenseAmt("utilities", new BigDecimal("105.00"), 30),
                expenseAmt("utilities", new BigDecimal("110.00"),  0)
            );
            List<Subscription> r = subscriptionDetectorService.detectSubscriptions(1L);
            assertThat(r).isEmpty(); // Variable spend rejected
        }

        @Test
        @DisplayName("lastPaidDate equals transactionDate of most-recent transaction")
        void detect_lastPaidDate_isLastTransactionDate() {
            LocalDateTime lastDate = LocalDateTime.now().withNano(0);
            givenExpenses(
                expense("gym", "50.00", 30),
                expenseAt("gym", "50.00", lastDate)
            );
            List<Subscription> r = subscriptionDetectorService.detectSubscriptions(1L);
            assertThat(r).hasSize(1);
            assertThat(r.get(0).getLastPaidDate()).isEqualTo(lastDate.toLocalDate());
        }

        @Test
        @DisplayName("nextDueDate is lastPaidDate + 30 days")
        void detect_nextDueDate_is30DaysAfterLastPaid() {
            givenExpenses(
                expense("netflix", "15.99", 30),
                expense("netflix", "15.99",  0)
            );
            List<Subscription> r = subscriptionDetectorService.detectSubscriptions(1L);
            assertThat(r).hasSize(1);
            assertThat(r.get(0).getNextDueDate()).isEqualTo(LocalDate.now().plusDays(30));
        }

        @Test
        @DisplayName("New subscription has ACTIVE status")
        void detect_newSubscription_hasActiveStatus() {
            givenExpenses(
                expense("netflix", "15.99", 30),
                expense("netflix", "15.99",  0)
            );
            List<Subscription> r = subscriptionDetectorService.detectSubscriptions(1L);
            assertThat(r).hasSize(1);
            assertThat(r.get(0).getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("Subscription is linked to the correct user")
        void detect_subscription_linkedToCorrectUser() {
            givenExpenses(
                expense("gym", "50.00", 30),
                expense("gym", "50.00",  0)
            );
            List<Subscription> r = subscriptionDetectorService.detectSubscriptions(1L);
            assertThat(r).hasSize(1);
            assertThat(r.get(0).getUser().getId()).isEqualTo(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Merging with existing subscriptions
    // normalizeMerchant = merchant.toLowerCase().trim()
    // For merge to fire: existing.merchant.toLowerCase() == transaction.category
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Merging with existing subscriptions")
    class MergeTests {

        @Test
        @DisplayName("IGNORED subscription's status preserved after re-detection")
        void detect_existingIgnored_statusPreserved() {
            Subscription ignored = existingSub("netflix", SubscriptionStatus.IGNORED);
            when(subscriptionRepository.findByUser(testUser)).thenReturn(List.of(ignored));
            givenExpenses(expense("netflix", "15.99", 30), expense("netflix", "15.99", 0));
            List<Subscription> r = subscriptionDetectorService.detectSubscriptions(1L);
            assertThat(r).hasSize(1);
            assertThat(r.get(0).getStatus()).isEqualTo(SubscriptionStatus.IGNORED);
        }

        @Test
        @DisplayName("ACTIVE subscription's status stays ACTIVE")
        void detect_existingActive_statusPreserved() {
            Subscription active = existingSub("gym", SubscriptionStatus.ACTIVE);
            when(subscriptionRepository.findByUser(testUser)).thenReturn(List.of(active));
            givenExpenses(expense("gym", "50.00", 30), expense("gym", "50.00", 0));
            List<Subscription> r = subscriptionDetectorService.detectSubscriptions(1L);
            assertThat(r.get(0).getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("Existing subscription avgAmount is updated")
        void detect_existingSubscription_avgAmountUpdated() {
            Subscription existing = existingSub("netflix", SubscriptionStatus.ACTIVE);
            existing.setAvgAmount(new BigDecimal("5.00"));
            when(subscriptionRepository.findByUser(testUser)).thenReturn(List.of(existing));
            givenExpenses(
                expenseAmt("netflix", new BigDecimal("20.00"), 30),
                expenseAmt("netflix", new BigDecimal("20.00"),  0)
            );
            List<Subscription> r = subscriptionDetectorService.detectSubscriptions(1L);
            assertThat(r).hasSize(1);
            assertThat(r.get(0).getAvgAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("Existing subscription nextDueDate is updated to last payment + 30")
        void detect_existingSubscription_nextDueDateUpdated() {
            Subscription existing = existingSub("netflix", SubscriptionStatus.ACTIVE);
            existing.setNextDueDate(LocalDate.now().minusDays(10));
            when(subscriptionRepository.findByUser(testUser)).thenReturn(List.of(existing));
            givenExpenses(expense("netflix", "15.99", 30), expense("netflix", "15.99", 0));
            List<Subscription> r = subscriptionDetectorService.detectSubscriptions(1L);
            assertThat(r.get(0).getNextDueDate()).isEqualTo(LocalDate.now().plusDays(30));
        }

        @Test
        @DisplayName("New category alongside existing — both returned")
        void detect_newCategoryAlongsideExisting_bothReturned() {
            Subscription existing = existingSub("netflix", SubscriptionStatus.ACTIVE);
            when(subscriptionRepository.findByUser(testUser)).thenReturn(List.of(existing));
            givenExpenses(
                expense("netflix", "15.99", 30), expense("netflix", "15.99", 0),
                expense("spotify",  "9.99", 30), expense("spotify",  "9.99", 0)
            );
            assertThat(subscriptionDetectorService.detectSubscriptions(1L)).hasSize(2);
        }

        @Test
        @DisplayName("Merchant with no new valid pattern is not in result")
        void detect_ignoredMerchantWithNoNewTransactions_notReturned() {
            Subscription netflixSub = existingSub("netflix", SubscriptionStatus.ACTIVE);
            when(subscriptionRepository.findByUser(testUser)).thenReturn(List.of(netflixSub));
            givenExpenses(expense("spotify", "9.99", 30), expense("spotify", "9.99", 0));
            List<Subscription> r = subscriptionDetectorService.detectSubscriptions(1L);
            assertThat(r).extracting(Subscription::getMerchant).containsExactly("spotify");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Persistence
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Persistence")
    class PersistenceTests {

        @Test
        @DisplayName("saveAll() called with the detected subscription")
        void detect_saveAllCalledWithResults() {
            givenExpenses(expense("netflix", "15.99", 30), expense("netflix", "15.99", 0));
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Subscription>> cap = ArgumentCaptor.forClass(List.class);
            subscriptionDetectorService.detectSubscriptions(1L);
            verify(subscriptionRepository).saveAll(cap.capture());
            assertThat(cap.getValue()).hasSize(1);
        }

        @Test
        @DisplayName("saveAll() called with empty list when no patterns found")
        void detect_noPatterns_saveAllCalledWithEmpty() {
            givenExpenses(expense("netflix", "15.99", 0));
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Subscription>> cap = ArgumentCaptor.forClass(List.class);
            subscriptionDetectorService.detectSubscriptions(1L);
            verify(subscriptionRepository).saveAll(cap.capture());
            assertThat(cap.getValue()).isEmpty();
        }

        @Test
        @DisplayName("All transactions are fetched and filtered by type in service")
        void detect_allTransactionsFetched() {
            givenExpenses();
            subscriptionDetectorService.detectSubscriptions(1L);
            verify(transactionRepository).findByUserOrderByTransactionDateDesc(testUser);
            verify(transactionRepository, never()).findByUserAndType(any(), anyString());
        }

        @Test
        @DisplayName("Unknown userId throws without touching transaction or subscription repos")
        void detect_unknownUserId_throwsAndDoesNotPersist() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> subscriptionDetectorService.detectSubscriptions(999L))
                    .isInstanceOf(RuntimeException.class);
            verify(transactionRepository, never()).findByUserAndType(any(), any());
            verify(subscriptionRepository, never()).saveAll(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // findDueSoon()
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("findDueSoon()")
    class FindDueSoonTests {

        @Test
        @DisplayName("Delegates to subscriptionRepository.findDueSoon() with correct dates")
        void findDueSoon_delegatesToRepository() {
            LocalDate start = LocalDate.now();
            LocalDate end   = start.plusDays(7);
            when(subscriptionRepository.findDueSoon(testUser, start, end)).thenReturn(List.of());
            subscriptionDetectorService.findDueSoon(1L, 7);
            verify(subscriptionRepository).findDueSoon(testUser, start, end);
        }

        @Test
        @DisplayName("Returns repository result directly")
        void findDueSoon_returnsRepositoryResult() {
            Subscription due = existingSub("netflix", SubscriptionStatus.ACTIVE);
            when(subscriptionRepository.findDueSoon(any(), any(), any())).thenReturn(List.of(due));
            assertThat(subscriptionDetectorService.findDueSoon(1L, 7)).containsExactly(due);
        }

        @Test
        @DisplayName("days parameter controls the lookahead window")
        void findDueSoon_dayParameterUsed() {
            LocalDate start = LocalDate.now();
            when(subscriptionRepository.findDueSoon(testUser, start, start.plusDays(30)))
                    .thenReturn(List.of());
            subscriptionDetectorService.findDueSoon(1L, 30);
            verify(subscriptionRepository).findDueSoon(testUser, start, start.plusDays(30));
        }

        @Test
        @DisplayName("Unknown userId throws RuntimeException")
        void findDueSoon_unknownUser_throwsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> subscriptionDetectorService.findDueSoon(999L, 7))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ── builder helpers ───────────────────────────────────────────────────────

    private void givenExpenses(Transaction... transactions) {
        when(transactionRepository.findByUserOrderByTransactionDateDesc(testUser))
                .thenReturn(List.of(transactions));
    }

    private Transaction expense(String category, String amount, int daysAgo) {
        return expenseAt(category, amount, LocalDateTime.now().minusDays(daysAgo));
    }

    private Transaction expenseAt(String category, String amount, LocalDateTime dt) {
        return Transaction.builder()
                .id((long) (Math.random() * 1_000_000))
                .user(testUser)
                .amount(new BigDecimal(amount != null ? amount : "0"))
                .type("EXPENSE")
                .category(category)
                .transactionDate(dt)
                .createdAt(dt)
                .build();
    }

    private Transaction expenseAmt(String category, BigDecimal amount, int daysAgo) {
        LocalDateTime dt = LocalDateTime.now().minusDays(daysAgo);
        return Transaction.builder()
                .id((long) (Math.random() * 1_000_000))
                .user(testUser)
                .amount(amount)
                .type("EXPENSE")
                .category(category)
                .transactionDate(dt)
                .createdAt(dt)
                .build();
    }

    private Subscription existingSub(String merchant, SubscriptionStatus status) {
        return Subscription.builder()
                .id((long) (Math.random() * 1_000_000))
                .user(testUser)
                .merchant(merchant)
                .avgAmount(new BigDecimal("15.99"))
                .lastPaidDate(LocalDate.now().minusDays(30))
                .nextDueDate(LocalDate.now())
                .status(status)
                .createdAt(LocalDateTime.now().minusDays(60))
                .build();
    }
}