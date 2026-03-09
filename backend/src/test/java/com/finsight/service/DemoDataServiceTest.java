package com.finsight.service;

import com.finsight.dto.FraudDetectionResult;
import com.finsight.model.FraudAlert;
import com.finsight.model.RiskLevel;
import com.finsight.model.Transaction;
import com.finsight.model.User;
import com.finsight.repository.FraudAlertRepository;
import com.finsight.repository.TransactionRepository;
import com.finsight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Demo Data Service Tests")
class DemoDataServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @InjectMocks
    private DemoDataService demoDataService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .password("password")
            .build();
    }

    @Test
    @DisplayName("Seed user if empty - creates transactions when user has none")
    void seedUserIfEmpty_NoExistingTransactions_CreatesTransactions() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.countByUser(testUser)).thenReturn(0L);
        when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(invocation -> {
                Transaction txn = invocation.getArgument(0);
                if (txn.getId() == null) {
                    txn.setId(1L);
                }
                return txn;
            });
        when(fraudDetectionService.analyzeTransaction(any(Transaction.class)))
            .thenReturn(FraudDetectionResult.builder()
                .fraudulent(false)
                .fraudScore(0.0)
                .riskLevel(RiskLevel.LOW)
                .reasons(List.of())
                .build());

        // When
        int count = demoDataService.seedUserIfEmpty(1L);

        // Then
        assertThat(count).isGreaterThan(0);
        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
        verify(fraudDetectionService, atLeastOnce()).analyzeTransaction(any(Transaction.class));
    }

    @Test
    @DisplayName("Seed user if empty - skips when user has transactions")
    void seedUserIfEmpty_ExistingTransactions_SkipsSeeding() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.countByUser(testUser)).thenReturn(10L);

        // When
        int count = demoDataService.seedUserIfEmpty(1L);

        // Then
        assertThat(count).isEqualTo(0);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Seed user if empty - user not found throws exception")
    void seedUserIfEmpty_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> demoDataService.seedUserIfEmpty(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Force reseed - deletes existing and creates new transactions")
    void forceReseedUser_DeletesAndCreatesNew() {
        // Given
        List<Transaction> existingTransactions = new ArrayList<>();
        existingTransactions.add(Transaction.builder().id(1L).user(testUser).build());
        
        List<FraudAlert> existingAlerts = new ArrayList<>();
        existingAlerts.add(FraudAlert.builder().id(1L).user(testUser).build());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fraudAlertRepository.findByUserOrderByCreatedAtDesc(testUser))
            .thenReturn(existingAlerts);
        when(transactionRepository.findByUserOrderByTransactionDateDesc(testUser))
            .thenReturn(existingTransactions);
        when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(invocation -> {
                Transaction txn = invocation.getArgument(0);
                if (txn.getId() == null) {
                    txn.setId(2L);
                }
                return txn;
            });
        when(fraudDetectionService.analyzeTransaction(any(Transaction.class)))
            .thenReturn(FraudDetectionResult.builder()
                .fraudulent(false)
                .fraudScore(0.0)
                .riskLevel(RiskLevel.LOW)
                .reasons(List.of())
                .build());

        // When
        int count = demoDataService.forceReseedUser(1L);

        // Then
        assertThat(count).isGreaterThan(0);
        verify(fraudAlertRepository).deleteAll(existingAlerts);
        verify(transactionRepository).deleteAll(existingTransactions);
        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Demo transactions span multiple months")
    void demoTransactions_SpanMultipleMonths() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.countByUser(testUser)).thenReturn(0L);
        
        List<Transaction> capturedTransactions = new ArrayList<>();
        when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(invocation -> {
                Transaction txn = invocation.getArgument(0);
                if (txn.getId() == null) {
                    txn.setId((long) (capturedTransactions.size() + 1));
                }
                capturedTransactions.add(txn);
                return txn;
            });
        
        when(fraudDetectionService.analyzeTransaction(any(Transaction.class)))
            .thenReturn(FraudDetectionResult.builder()
                .fraudulent(false)
                .fraudScore(0.0)
                .riskLevel(RiskLevel.LOW)
                .reasons(List.of())
                .build());

        // When
        demoDataService.seedUserIfEmpty(1L);

        // Then
        assertThat(capturedTransactions).isNotEmpty();
        
        // Check that transactions span multiple months
        long distinctMonths = capturedTransactions.stream()
            .map(txn -> txn.getTransactionDate().getMonth())
            .distinct()
            .count();
        
        assertThat(distinctMonths).isGreaterThan(1);
    }

    @Test
    @DisplayName("Demo transactions include fraud scenarios")
    void demoTransactions_IncludeFraudScenarios() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.countByUser(testUser)).thenReturn(0L);
        
        List<Transaction> capturedTransactions = new ArrayList<>();
        when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(invocation -> {
                Transaction txn = invocation.getArgument(0);
                if (txn.getId() == null) {
                    txn.setId((long) (capturedTransactions.size() + 1));
                }
                capturedTransactions.add(txn);
                return txn;
            });
        
        // Return high fraud score for some transactions
        when(fraudDetectionService.analyzeTransaction(any(Transaction.class)))
            .thenAnswer(invocation -> {
                Transaction txn = invocation.getArgument(0);
                boolean isFraud = txn.getAmount().compareTo(BigDecimal.valueOf(1000)) > 0;
                return FraudDetectionResult.builder()
                    .fraudulent(isFraud)
                    .fraudScore(isFraud ? 85.0 : 0.0)
                    .riskLevel(isFraud ? RiskLevel.HIGH : RiskLevel.LOW)
                    .reasons(isFraud ? List.of("High amount") : List.of())
                    .build();
            });

        // When
        demoDataService.seedUserIfEmpty(1L);

        // Then
        assertThat(capturedTransactions).isNotEmpty();
        
        // Check that some transactions have high amounts (fraud triggers)
        long highAmountTransactions = capturedTransactions.stream()
            .filter(txn -> txn.getAmount().compareTo(BigDecimal.valueOf(1000)) > 0)
            .count();
        
        assertThat(highAmountTransactions).isGreaterThan(0);
    }

    @Test
    @DisplayName("Fraud alerts created for fraudulent transactions")
    void fraudAlerts_CreatedForFraudulentTransactions() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.countByUser(testUser)).thenReturn(0L);
        when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(invocation -> {
                Transaction txn = invocation.getArgument(0);
                if (txn.getId() == null) {
                    txn.setId(1L);
                }
                return txn;
            });
        
        // Return fraud result with score > 0
        when(fraudDetectionService.analyzeTransaction(any(Transaction.class)))
            .thenReturn(FraudDetectionResult.builder()
                .fraudulent(true)
                .fraudScore(85.0)
                .riskLevel(RiskLevel.HIGH)
                .reasons(List.of("High amount anomaly"))
                .build());

        // When
        demoDataService.seedUserIfEmpty(1L);

        // Then
        verify(fraudAlertRepository, atLeastOnce()).save(any(FraudAlert.class));
    }

    @Test
    @DisplayName("Transactions have valid categories")
    void transactions_HaveValidCategories() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.countByUser(testUser)).thenReturn(0L);
        
        List<Transaction> capturedTransactions = new ArrayList<>();
        when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(invocation -> {
                Transaction txn = invocation.getArgument(0);
                if (txn.getId() == null) {
                    txn.setId((long) (capturedTransactions.size() + 1));
                }
                capturedTransactions.add(txn);
                return txn;
            });
        
        when(fraudDetectionService.analyzeTransaction(any(Transaction.class)))
            .thenReturn(FraudDetectionResult.builder()
                .fraudulent(false)
                .fraudScore(0.0)
                .riskLevel(RiskLevel.LOW)
                .reasons(List.of())
                .build());

        // When
        demoDataService.seedUserIfEmpty(1L);

        // Then
        assertThat(capturedTransactions).isNotEmpty();
        assertThat(capturedTransactions).allMatch(txn -> txn.getCategory() != null && !txn.getCategory().isEmpty());
    }

    @Test
    @DisplayName("Transactions have valid amounts")
    void transactions_HaveValidAmounts() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.countByUser(testUser)).thenReturn(0L);
        
        List<Transaction> capturedTransactions = new ArrayList<>();
        when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(invocation -> {
                Transaction txn = invocation.getArgument(0);
                if (txn.getId() == null) {
                    txn.setId((long) (capturedTransactions.size() + 1));
                }
                capturedTransactions.add(txn);
                return txn;
            });
        
        when(fraudDetectionService.analyzeTransaction(any(Transaction.class)))
            .thenReturn(FraudDetectionResult.builder()
                .fraudulent(false)
                .fraudScore(0.0)
                .riskLevel(RiskLevel.LOW)
                .reasons(List.of())
                .build());

        // When
        demoDataService.seedUserIfEmpty(1L);

        // Then
        assertThat(capturedTransactions).isNotEmpty();
        assertThat(capturedTransactions).allMatch(txn -> 
            txn.getAmount() != null && txn.getAmount().compareTo(BigDecimal.ZERO) >= 0
        );
    }

    @Test
    @DisplayName("Deterministic generation based on user ID")
    void generation_IsDeterministic() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.countByUser(testUser)).thenReturn(0L);
        
        List<Transaction> firstRun = new ArrayList<>();
        when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(invocation -> {
                Transaction txn = invocation.getArgument(0);
                if (txn.getId() == null) {
                    txn.setId((long) (firstRun.size() + 1));
                }
                firstRun.add(txn);
                return txn;
            });
        
        when(fraudDetectionService.analyzeTransaction(any(Transaction.class)))
            .thenReturn(FraudDetectionResult.builder()
                .fraudulent(false)
                .fraudScore(0.0)
                .riskLevel(RiskLevel.LOW)
                .reasons(List.of())
                .build());

        // When
        int count1 = demoDataService.seedUserIfEmpty(1L);

        // Reset for second run
        List<Transaction> secondRun = new ArrayList<>();
        when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(invocation -> {
                Transaction txn = invocation.getArgument(0);
                if (txn.getId() == null) {
                    txn.setId((long) (secondRun.size() + 1));
                }
                secondRun.add(txn);
                return txn;
            });

        int count2 = demoDataService.seedUserIfEmpty(1L);

        // Then
        assertThat(count1).isEqualTo(count2);
        assertThat(firstRun).hasSameSizeAs(secondRun);
    }
}
