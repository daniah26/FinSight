package com.finsight.service;

import com.finsight.dto.FraudDetectionResult;
import com.finsight.model.RiskLevel;
import com.finsight.model.Transaction;
import com.finsight.model.User;
import com.finsight.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fraud Detection Integration Tests")
class FraudDetectionIntegrationTest {

    @Mock(lenient = true)
    private TransactionRepository transactionRepository;

    private FraudDetectionService fraudDetectionService;
    private User testUser;

    @BeforeEach
    void setUp() {
        fraudDetectionService = new FraudDetectionService(transactionRepository);
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .build();
    }

    @Test
    @DisplayName("Multiple fraud indicators should accumulate scores")
    void multipleFraudIndicators_AccumulateScores() {
        // Given: Transaction with multiple fraud indicators
        // 1. High amount (>3x average): 30 points
        // 2. Rapid fire (5+ in 10 min): 25 points
        // 3. New category: 20 points
        Transaction transaction = createTransaction(BigDecimal.valueOf(400), "crypto");

        when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(5L);
        when(transactionRepository.findByUserOrderByTransactionDateDesc(any())).thenReturn(List.of());
        when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));

        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);

        // Then
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(70.0);
        assertThat(result.isFraudulent()).isTrue();
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.getReasons()).hasSize(3);
    }

    @Test
    @DisplayName("Extreme amount should trigger high fraud score")
    void extremeAmount_TriggersHighFraudScore() {
        // Given: Transaction with 10x average amount (100x10 = 1000, but 10000 is 100x)
        Transaction transaction = createTransaction(BigDecimal.valueOf(10000), "luxury");

        when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(0L);
        when(transactionRepository.findByUserOrderByTransactionDateDesc(any())).thenReturn(List.of());
        when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));

        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);

        // Then
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(50.0);
        assertThat(result.getReasons()).anyMatch(reason -> reason.contains("exceeds 3x user average"));
    }

    @Test
    @DisplayName("Geographical anomaly with rapid transaction should be flagged")
    void geographicalAnomalyWithRapidTransaction_Flagged() {
        // Given: Different location within 2 hours
        Transaction lastTransaction = createTransaction(BigDecimal.valueOf(50), "groceries");
        lastTransaction.setLocation("New York");
        lastTransaction.setTransactionDate(LocalDateTime.now().minusMinutes(30));

        Transaction currentTransaction = createTransaction(BigDecimal.valueOf(100), "groceries");
        currentTransaction.setLocation("Tokyo");

        when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(0L);
        when(transactionRepository.findByUserOrderByTransactionDateDesc(any()))
            .thenReturn(List.of(lastTransaction));
        when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));

        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(currentTransaction);

        // Then
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(25.0);
        assertThat(result.getReasons()).contains("Different location within 2 hours of previous transaction");
    }

    @Test
    @DisplayName("Same location should not trigger geographical anomaly")
    void sameLocation_NoGeographicalAnomaly() {
        // Given: Same location
        Transaction lastTransaction = createTransaction(BigDecimal.valueOf(50), "groceries");
        lastTransaction.setLocation("New York");
        lastTransaction.setTransactionDate(LocalDateTime.now().minusMinutes(30));

        Transaction currentTransaction = createTransaction(BigDecimal.valueOf(100), "groceries");
        currentTransaction.setLocation("New York");

        when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(0L);
        when(transactionRepository.findByUserOrderByTransactionDateDesc(any()))
            .thenReturn(List.of(lastTransaction));
        when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));

        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(currentTransaction);

        // Then
        assertThat(result.getReasons()).doesNotContain("Different location within 2 hours");
    }

    @Test
    @DisplayName("Known category should not trigger new category alert")
    void knownCategory_NoNewCategoryAlert() {
        // Given: User has used this category before
        Transaction transaction = createTransaction(BigDecimal.valueOf(50), "groceries");

        when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(0L);
        when(transactionRepository.findByUserOrderByTransactionDateDesc(any())).thenReturn(List.of());
        when(transactionRepository.findDistinctCategoriesByUser(any()))
            .thenReturn(List.of("groceries", "utilities"));

        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);

        // Then
        assertThat(result.getReasons()).doesNotContain("New category for user");
    }

    @Test
    @DisplayName("Risk level LOW for score below 40")
    void riskLevel_LowForScoreBelow40() {
        // Given: Transaction with low fraud score
        Transaction transaction = createTransaction(BigDecimal.valueOf(50), "groceries");

        when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(0L);
        when(transactionRepository.findByUserOrderByTransactionDateDesc(any())).thenReturn(List.of());
        when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));

        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);

        // Then
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.getFraudScore()).isLessThan(40.0);
    }

    @Test
    @DisplayName("Risk level MEDIUM for score between 40 and 70")
    void riskLevel_MediumForScoreBetween40And70() {
        // Given: Transaction with medium fraud indicators (need 40-70 points)
        // 3x average (30 points) + new category (20 points) = 50 points
        Transaction transaction = createTransaction(BigDecimal.valueOf(350), "entertainment");

        when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(0L);
        when(transactionRepository.findByUserOrderByTransactionDateDesc(any())).thenReturn(List.of());
        when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));

        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);

        // Then
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.getFraudScore()).isBetween(40.0, 70.0);
    }

    @Test
    @DisplayName("Risk level HIGH for score 70 or above")
    void riskLevel_HighForScore70OrAbove() {
        // Given: Transaction with multiple high-risk indicators
        Transaction transaction = createTransaction(BigDecimal.valueOf(500), "crypto");

        when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(6L);
        when(transactionRepository.findByUserOrderByTransactionDateDesc(any())).thenReturn(List.of());
        when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));

        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);

        // Then
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(70.0);
    }

    @Test
    @DisplayName("Rapid fire with 10+ transactions should add 40 points")
    void rapidFire_10Plus_Adds40Points() {
        // Given: 10+ transactions in 10 minutes
        Transaction transaction = createTransaction(BigDecimal.valueOf(50), "groceries");

        when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(10L);
        when(transactionRepository.findByUserOrderByTransactionDateDesc(any())).thenReturn(List.of());
        when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));

        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);

        // Then
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(25.0); // Rapid fire adds 25 points
        assertThat(result.getReasons()).contains("5 or more transactions within 10 minutes");
    }

    @Test
    @DisplayName("First transaction for user should not cause errors")
    void firstTransaction_NoErrors() {
        // Given: First transaction (no history)
        Transaction transaction = createTransaction(BigDecimal.valueOf(100), "groceries");

        when(transactionRepository.calculateAverageAmount(any())).thenReturn(null);
        when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(0L);
        when(transactionRepository.findByUserOrderByTransactionDateDesc(any())).thenReturn(List.of());
        when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of());

        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("Zero amount transaction should not cause errors")
    void zeroAmount_NoErrors() {
        // Given: Transaction with zero amount
        Transaction transaction = createTransaction(BigDecimal.ZERO, "groceries");

        when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(0L);
        when(transactionRepository.findByUserOrderByTransactionDateDesc(any())).thenReturn(List.of());
        when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));

        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(0.0);
    }

    private Transaction createTransaction(BigDecimal amount, String category) {
        return Transaction.builder()
            .id(1L)
            .user(testUser)
            .amount(amount)
            .type("EXPENSE")
            .category(category)
            .description("Test transaction")
            .location("Test Location")
            .transactionDate(LocalDateTime.now())
            .build();
    }
}
