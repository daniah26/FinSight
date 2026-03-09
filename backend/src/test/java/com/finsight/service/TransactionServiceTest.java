package com.finsight.service;

import com.finsight.dto.FraudDetectionResult;
import com.finsight.dto.TransactionRequest;
import com.finsight.dto.TransactionResponse;
import com.finsight.model.FraudAlert;
import com.finsight.model.RiskLevel;
import com.finsight.model.Transaction;
import com.finsight.model.User;
import com.finsight.repository.FraudAlertRepository;
import com.finsight.repository.TransactionRepository;
import com.finsight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .password("password")
            .build();

        testTransaction = Transaction.builder()
            .id(1L)
            .user(testUser)
            .amount(BigDecimal.valueOf(100))
            .type("EXPENSE")
            .category("groceries")
            .description("Test transaction")
            .location("Test Location")
            .transactionDate(LocalDateTime.now())
            .fraudulent(false)
            .fraudScore(0.0)
            .build();
    }

    @Test
    void createTransaction_ValidRequest_CreatesAndReturnTransaction() {
        // Given
        TransactionRequest request = new TransactionRequest();
        request.setUserId(1L);
        request.setAmount(BigDecimal.valueOf(100));
        request.setType("EXPENSE");
        request.setCategory("groceries");
        request.setDescription("Test transaction");
        request.setLocation("Test Location");

        FraudDetectionResult fraudResult = FraudDetectionResult.builder()
            .fraudulent(false)
            .fraudScore(0.0)
            .riskLevel(RiskLevel.LOW)
            .reasons(List.of())
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(fraudDetectionService.analyzeTransaction(any(Transaction.class))).thenReturn(fraudResult);

        // When
        TransactionResponse response = transactionService.createTransaction(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(response.getCategory()).isEqualTo("groceries");
        assertThat(response.isFraudulent()).isFalse();
        
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(fraudDetectionService).analyzeTransaction(any(Transaction.class));
    }

    @Test
    void createTransaction_FraudDetected_MarksFraudulent() {
        // Given
        TransactionRequest request = new TransactionRequest();
        request.setUserId(1L);
        request.setAmount(BigDecimal.valueOf(5000));
        request.setType("EXPENSE");
        request.setCategory("luxury");

        FraudDetectionResult fraudResult = FraudDetectionResult.builder()
            .fraudulent(true)
            .fraudScore(85.0)
            .riskLevel(RiskLevel.HIGH)
            .reasons(List.of("High amount anomaly"))
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fraudDetectionService.analyzeTransaction(any(Transaction.class))).thenReturn(fraudResult);
        
        Transaction fraudTransaction = Transaction.builder()
            .id(2L)
            .user(testUser)
            .amount(BigDecimal.valueOf(5000))
            .fraudulent(true)
            .fraudScore(85.0)
            .build();
        
        when(transactionRepository.save(any(Transaction.class))).thenReturn(fraudTransaction);

        // When
        TransactionResponse response = transactionService.createTransaction(request);

        // Then
        assertThat(response.isFraudulent()).isTrue();
        assertThat(response.getFraudScore()).isEqualTo(85.0);
        
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        
        Transaction savedTransaction = captor.getAllValues().get(1);
        assertThat(savedTransaction.isFraudulent()).isTrue();
        assertThat(savedTransaction.getFraudScore()).isEqualTo(85.0);
    }

    @Test
    void createTransaction_UserNotFound_ThrowsException() {
        // Given
        TransactionRequest request = new TransactionRequest();
        request.setUserId(999L);
        request.setAmount(BigDecimal.valueOf(100));
        
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> transactionService.createTransaction(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void findWithFilters_ReturnsFilteredTransactions() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("transactionDate").descending());
        Page<Transaction> transactionPage = mock(Page.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(transactionPage);
        when(transactionPage.map(any())).thenReturn(Page.empty());

        // When
        Page<TransactionResponse> responses = transactionService.findWithFilters(
            1L, "EXPENSE", "groceries", null, null, false, 
            "transactionDate", "desc", 0, 10
        );

        // Then
        assertThat(responses).isNotNull();
        verify(transactionRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void findWithFilters_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> transactionService.findWithFilters(
            999L, null, null, null, null, null, "transactionDate", "desc", 0, 10
        ))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void createTransaction_SetsTransactionDateToNow() {
        // Given
        TransactionRequest request = new TransactionRequest();
        request.setUserId(1L);
        request.setAmount(BigDecimal.valueOf(100));
        request.setType("EXPENSE");
        request.setCategory("groceries");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(fraudDetectionService.analyzeTransaction(any(Transaction.class)))
            .thenReturn(FraudDetectionResult.builder()
                .fraudulent(false)
                .fraudScore(0.0)
                .riskLevel(RiskLevel.LOW)
                .reasons(List.of())
                .build());

        // When
        transactionService.createTransaction(request);

        // Then
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, atLeastOnce()).save(captor.capture());
        
        Transaction savedTransaction = captor.getValue();
        assertThat(savedTransaction.getCreatedAt()).isNotNull();
    }

    @Test
    void createTransaction_HighFraudScore_CreatesFraudAlert() {
        // Given
        TransactionRequest request = new TransactionRequest();
        request.setUserId(1L);
        request.setAmount(BigDecimal.valueOf(5000));
        request.setType("EXPENSE");
        request.setCategory("luxury");

        FraudDetectionResult fraudResult = FraudDetectionResult.builder()
            .fraudulent(true)
            .fraudScore(85.0)
            .riskLevel(RiskLevel.HIGH)
            .reasons(List.of("High amount anomaly"))
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fraudDetectionService.analyzeTransaction(any(Transaction.class))).thenReturn(fraudResult);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        transactionService.createTransaction(request);

        // Then
        verify(fraudAlertRepository).save(any(FraudAlert.class));
    }
}
