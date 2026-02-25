package com.finsight.service;

import com.finsight.dto.FraudDetectionResult;
import com.finsight.model.RiskLevel;
import com.finsight.model.Transaction;
import com.finsight.model.User;
import com.finsight.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {
    
    @Mock
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
            .password("password")
            .build();
    }
    
    @Test
    void analyzeTransaction_HighAmountAnomaly_Adds30Points() {
        // Given: User average is $100, transaction is $400 (>3x)
        Transaction transaction = createTransaction(BigDecimal.valueOf(400), "groceries");
        lenient().when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        lenient().when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(0L);
        lenient().when(transactionRepository.findTopByUserOrderByTransactionDateDesc(any())).thenReturn(Optional.empty());
        lenient().when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));
        
        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);
        
        // Then
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(30.0);
        assertThat(result.getReasons()).contains("Amount exceeds 3x user average");
    }
    
    @Test
    void analyzeTransaction_RapidFireActivity_Adds25Points() {
        // Given: 5+ transactions in last 10 minutes
        Transaction transaction = createTransaction(BigDecimal.valueOf(50), "groceries");
        lenient().when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        lenient().when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(5L);
        lenient().when(transactionRepository.findTopByUserOrderByTransactionDateDesc(any())).thenReturn(Optional.empty());
        lenient().when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));
        
        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);
        
        // Then
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(25.0);
        assertThat(result.getReasons()).contains("5+ transactions in 10 minutes");
    }
    
    @Test
    void analyzeTransaction_GeographicalAnomaly_Adds25Points() {
        // Given: Last transaction in different location < 2 hours ago
        Transaction lastTransaction = createTransaction(BigDecimal.valueOf(50), "groceries");
        lastTransaction.setLocation("New York");
        lastTransaction.setTransactionDate(LocalDateTime.now().minusHours(1));
        
        Transaction currentTransaction = createTransaction(BigDecimal.valueOf(50), "groceries");
        currentTransaction.setLocation("Los Angeles");
        
        when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(0L);
        when(transactionRepository.findTopByUserOrderByTransactionDateDesc(any())).thenReturn(Optional.of(lastTransaction));
        when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));
        
        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(currentTransaction);
        
        // Then
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(25.0);
        assertThat(result.getReasons()).contains("Different location within 2 hours");
    }
    
    @Test
    void analyzeTransaction_UnusualCategory_Adds20Points() {
        // Given: User has never used "entertainment" category
        Transaction transaction = createTransaction(BigDecimal.valueOf(50), "entertainment");
        lenient().when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        lenient().when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(0L);
        lenient().when(transactionRepository.findTopByUserOrderByTransactionDateDesc(any())).thenReturn(Optional.empty());
        lenient().when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries", "utilities"));
        
        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);
        
        // Then
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(20.0);
        assertThat(result.getReasons()).contains("New category for user");
    }
    
    @Test
    void analyzeTransaction_NoFraudIndicators_ReturnsZeroScore() {
        // Given: Normal transaction
        Transaction transaction = createTransaction(BigDecimal.valueOf(50), "groceries");
        lenient().when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        lenient().when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(0L);
        lenient().when(transactionRepository.findTopByUserOrderByTransactionDateDesc(any())).thenReturn(Optional.empty());
        lenient().when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));
        
        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);
        
        // Then
        assertThat(result.getFraudScore()).isEqualTo(0.0);
        assertThat(result.isFraudulent()).isFalse();
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.LOW);
    }
    
    @Test
    void analyzeTransaction_HighScore_SetsFraudulentFlag() {
        // Given: Transaction triggers multiple rules (score >= 70)
        Transaction transaction = createTransaction(BigDecimal.valueOf(400), "entertainment");
        lenient().when(transactionRepository.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        lenient().when(transactionRepository.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(5L);
        lenient().when(transactionRepository.findTopByUserOrderByTransactionDateDesc(any())).thenReturn(Optional.empty());
        lenient().when(transactionRepository.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));
        
        // When
        FraudDetectionResult result = fraudDetectionService.analyzeTransaction(transaction);
        
        // Then
        assertThat(result.getFraudScore()).isGreaterThanOrEqualTo(70.0);
        assertThat(result.isFraudulent()).isTrue();
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
    }
    
    private Transaction createTransaction(BigDecimal amount, String category) {
        return Transaction.builder()
            .id(1L)
            .user(testUser)
            .amount(amount)
            .type("EXPENSE")
            .category(category)
            .transactionDate(LocalDateTime.now())
            .build();
    }
}
