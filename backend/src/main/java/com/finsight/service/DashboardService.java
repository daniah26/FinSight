package com.finsight.service;

import com.finsight.dto.DashboardSummary;
import com.finsight.dto.TimeSeriesPoint;
import com.finsight.model.Transaction;
import com.finsight.model.User;
import com.finsight.repository.TransactionRepository;
import com.finsight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {
    
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    
    /**
     * Generates dashboard summary with aggregated metrics.
     * 
     * @param userId The user
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @return DashboardSummary with all metrics
     */
    public DashboardSummary getSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        List<Transaction> transactions = getFilteredTransactions(user, startDate, endDate);
        
        BigDecimal totalIncome = calculateTotalIncome(transactions);
        BigDecimal totalExpenses = calculateTotalExpenses(transactions);
        BigDecimal currentBalance = totalIncome.subtract(totalExpenses);
        
        Long totalFlaggedTransactions = countFlaggedTransactions(transactions);
        Double averageFraudScore = calculateAverageFraudScore(transactions);
        
        Map<String, BigDecimal> spendingByCategory = getSpendingByCategory(transactions);
        Map<String, Long> fraudByCategory = getFraudByCategory(transactions);
        List<TimeSeriesPoint> spendingTrends = getSpendingTrends(transactions);
        
        return DashboardSummary.builder()
            .totalIncome(totalIncome)
            .totalExpenses(totalExpenses)
            .currentBalance(currentBalance)
            .totalFlaggedTransactions(totalFlaggedTransactions)
            .averageFraudScore(averageFraudScore)
            .spendingByCategory(spendingByCategory)
            .fraudByCategory(fraudByCategory)
            .spendingTrends(spendingTrends)
            .build();
    }
    
    private List<Transaction> getFilteredTransactions(User user, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            return transactionRepository.findByUserAndTransactionDateBetween(user, start, end);
        }
        return transactionRepository.findByUserOrderByTransactionDateDesc(user);
    }
    
    private BigDecimal calculateTotalIncome(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> "INCOME".equals(t.getType()))
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calculateTotalExpenses(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> "EXPENSE".equals(t.getType()))
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private Long countFlaggedTransactions(List<Transaction> transactions) {
        return transactions.stream()
            .filter(Transaction::isFraudulent)
            .count();
    }
    
    private Double calculateAverageFraudScore(List<Transaction> transactions) {
        List<Double> scores = transactions.stream()
            .map(Transaction::getFraudScore)
            .filter(score -> score != null)
            .toList();
        
        if (scores.isEmpty()) {
            return 0.0;
        }
        
        double sum = scores.stream().mapToDouble(Double::doubleValue).sum();
        return BigDecimal.valueOf(sum / scores.size())
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }
    
    /**
     * Calculates spending by category.
     */
    private Map<String, BigDecimal> getSpendingByCategory(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> "EXPENSE".equals(t.getType()))
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    Transaction::getAmount,
                    BigDecimal::add
                )
            ));
    }
    
    /**
     * Calculates fraud incidents by category.
     */
    private Map<String, Long> getFraudByCategory(List<Transaction> transactions) {
        return transactions.stream()
            .filter(Transaction::isFraudulent)
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.counting()
            ));
    }
    
    /**
     * Calculates spending trends over time.
     */
    private List<TimeSeriesPoint> getSpendingTrends(List<Transaction> transactions) {
        Map<LocalDate, BigDecimal> dailySpending = transactions.stream()
            .filter(t -> "EXPENSE".equals(t.getType()))
            .collect(Collectors.groupingBy(
                t -> t.getTransactionDate().toLocalDate(),
                Collectors.reducing(
                    BigDecimal.ZERO,
                    Transaction::getAmount,
                    BigDecimal::add
                )
            ));
        
        return dailySpending.entrySet().stream()
            .map(entry -> TimeSeriesPoint.builder()
                .date(entry.getKey())
                .amount(entry.getValue())
                .build())
            .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
            .collect(Collectors.toList());
    }
}
