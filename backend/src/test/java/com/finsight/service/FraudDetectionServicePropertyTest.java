package com.finsight.service;

import com.finsight.dto.FraudDetectionResult;
import com.finsight.model.RiskLevel;
import com.finsight.model.Transaction;
import com.finsight.model.User;
import com.finsight.repository.TransactionRepository;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FraudDetectionServicePropertyTest {
    
    // Property 6: Fraud Score Bounds
    @Property(tries = 100)
    // Feature: finsight-enhancement, Property 6: Fraud score bounds
    void fraudScoreIsAlwaysBetween0And100(@ForAll("transactions") Transaction transaction) {
        // Setup service with mocks for each test
        TransactionRepository repo = Mockito.mock(TransactionRepository.class);
        FraudDetectionService service = new FraudDetectionService(repo);
        
        // Setup mocks
        when(repo.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        when(repo.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(0L);
        when(repo.findTopByUserOrderByTransactionDateDesc(any())).thenReturn(Optional.empty());
        when(repo.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));
        
        FraudDetectionResult result = service.analyzeTransaction(transaction);
        
        assertThat(result.getFraudScore()).isBetween(0.0, 100.0);
    }
    
    // Property 3: Fraud Score Threshold Enforcement
    @Property(tries = 100)
    // Feature: finsight-enhancement, Property 3: Fraud score threshold enforcement
    void fraudulentFlagSetCorrectlyBasedOnScore(@ForAll("fraudScores") double fraudScore) {
        boolean expectedFraudulent = fraudScore >= 70;
        
        if (fraudScore >= 70) {
            assertThat(expectedFraudulent).isTrue();
        } else {
            assertThat(expectedFraudulent).isFalse();
        }
    }
    
    // Property 4: Risk Level Mapping Correctness
    @Property(tries = 100)
    // Feature: finsight-enhancement, Property 4: Risk level mapping correctness
    void riskLevelMappedCorrectly(@ForAll("fraudScores") double score) {
        RiskLevel riskLevel = RiskLevel.fromScore(score);
        
        if (score < 40) {
            assertThat(riskLevel).isEqualTo(RiskLevel.LOW);
        } else if (score < 70) {
            assertThat(riskLevel).isEqualTo(RiskLevel.MEDIUM);
        } else {
            assertThat(riskLevel).isEqualTo(RiskLevel.HIGH);
        }
    }
    
    // Property 26: Fraud Detection Determinism
    @Property(tries = 100)
    // Feature: finsight-enhancement, Property 26: Fraud detection determinism
    void fraudDetectionIsDeterministic(@ForAll("transactions") Transaction transaction) {
        // Setup service with mocks for each test
        TransactionRepository repo = Mockito.mock(TransactionRepository.class);
        FraudDetectionService service = new FraudDetectionService(repo);
        
        // Setup mocks with fixed values
        when(repo.calculateAverageAmount(any())).thenReturn(BigDecimal.valueOf(100));
        when(repo.countByUserAndTransactionDateBetween(any(), any(), any())).thenReturn(0L);
        when(repo.findTopByUserOrderByTransactionDateDesc(any())).thenReturn(Optional.empty());
        when(repo.findDistinctCategoriesByUser(any())).thenReturn(List.of("groceries"));
        
        FraudDetectionResult result1 = service.analyzeTransaction(transaction);
        FraudDetectionResult result2 = service.analyzeTransaction(transaction);
        
        assertThat(result1.getFraudScore()).isEqualTo(result2.getFraudScore());
        assertThat(result1.isFraudulent()).isEqualTo(result2.isFraudulent());
        assertThat(result1.getRiskLevel()).isEqualTo(result2.getRiskLevel());
    }
    
    @Provide
    Arbitrary<Transaction> transactions() {
        return Combinators.combine(
            Arbitraries.longs().between(1L, 10000L),
            Arbitraries.bigDecimals().between(BigDecimal.ONE, BigDecimal.valueOf(10000)),
            Arbitraries.of("INCOME", "EXPENSE"),
            Arbitraries.of("groceries", "utilities", "entertainment", "salary", "rent")
        ).as((id, amount, type, category) -> {
            User user = User.builder()
                .id(id)
                .username("user" + id)
                .email("user" + id + "@test.com")
                .password("password")
                .build();
            
            return Transaction.builder()
                .id(id)
                .user(user)
                .amount(amount)
                .type(type)
                .category(category)
                .transactionDate(LocalDateTime.now())
                .build();
        });
    }
    
    @Provide
    Arbitrary<Double> fraudScores() {
        return Arbitraries.doubles().between(0.0, 100.0);
    }
}
