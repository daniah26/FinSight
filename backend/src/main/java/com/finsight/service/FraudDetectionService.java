package com.finsight.service;

import com.finsight.dto.FraudDetectionResult;
import com.finsight.model.RiskLevel;
import com.finsight.model.Transaction;
import com.finsight.model.User;
import com.finsight.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {
    
    private final TransactionRepository transactionRepository;
    
    /**
     * Analyzes a transaction and computes fraud score using rule-based algorithm.
     * 
     * Rules:
     * - High amount anomaly (>3x avg): +30 points
     * - Rapid-fire activity (5+ in 10 min): +25 points
     * - Geographical anomaly (different location < 2 hours): +25 points
     * - Unusual category (never used): +20 points
     * 
     * @param transaction The transaction to analyze (already saved in database)
     * @return FraudDetectionResult with score, risk level, and reasons
     */
    public FraudDetectionResult analyzeTransaction(Transaction transaction) {
        double score = 0.0;
        List<String> reasons = new ArrayList<>();
        
        User user = transaction.getUser();
        LocalDateTime transactionTime = transaction.getTransactionDate();
        
        log.info("=== Starting fraud detection for transaction {} ===", transaction.getId());
        log.info("Amount: {}, Category: {}, Location: {}, Time: {}", 
            transaction.getAmount(), transaction.getCategory(), 
            transaction.getLocation(), transactionTime);
        
        // Rule 1: High Amount Anomaly (>3x average) - +30 points
        BigDecimal userAvg = calculateUserAverage(user);
        log.info("Rule 1 - High Amount: User average = {}", userAvg);
        if (userAvg != null && userAvg.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal threshold = userAvg.multiply(BigDecimal.valueOf(3));
            log.info("Rule 1 - Threshold (3x avg) = {}, Transaction amount = {}", threshold, transaction.getAmount());
            if (transaction.getAmount().compareTo(threshold) > 0) {
                score += 30;
                reasons.add(String.format("Amount $%.2f exceeds 3x user average $%.2f", 
                    transaction.getAmount(), userAvg));
                log.warn("Rule 1 TRIGGERED: High amount anomaly (+30 points)");
            } else {
                log.info("Rule 1 NOT triggered: Amount within normal range");
            }
        } else {
            log.info("Rule 1 SKIPPED: No previous transactions to calculate average");
        }
        
        // Rule 2: Rapid-Fire Activity (5+ transactions in 10 minutes) - +25 points
        log.info("Rule 2 - Checking rapid-fire activity...");
        if (hasRapidFireActivity(user, transactionTime)) {
            score += 25;
            reasons.add("5 or more transactions within 10 minutes");
            log.warn("Rule 2 TRIGGERED: Rapid-fire activity (+25 points)");
        } else {
            log.info("Rule 2 NOT triggered: Less than 5 transactions in 10-minute window");
        }
        
        // Rule 3: Geographical Anomaly (different location < 2 hours) - +25 points
        log.info("Rule 3 - Checking geographical anomaly, location: {}", transaction.getLocation());
        if (transaction.getLocation() != null && !transaction.getLocation().isBlank()) {
            if (hasGeographicalAnomaly(user, transaction.getLocation(), transactionTime)) {
                score += 25;
                reasons.add("Different location within 2 hours of previous transaction");
                log.warn("Rule 3 TRIGGERED: Geographical anomaly (+25 points)");
            } else {
                log.info("Rule 3 NOT triggered: No geographical anomaly detected");
            }
        } else {
            log.info("Rule 3 SKIPPED: No location provided");
        }
        
        // Rule 4: Unusual Category (never used before) - +20 points
        log.info("Rule 4 - Checking unusual category: {}", transaction.getCategory());
        if (isUnusualCategory(user, transaction.getCategory())) {
            score += 20;
            reasons.add(String.format("First time using category: %s", transaction.getCategory()));
            log.warn("Rule 4 TRIGGERED: Unusual category (+20 points)");
        } else {
            log.info("Rule 4 NOT triggered: Category has been used before");
        }
        
        // Ensure score is within bounds [0, 100]
        score = Math.min(100.0, Math.max(0.0, score));
        
        RiskLevel riskLevel = calculateRiskLevel(score);
        // Mark as fraudulent only if:
        // 1. Transaction is an EXPENSE (not INCOME)
        // 2. Score >= 40 (MEDIUM or HIGH severity)
        boolean fraudulent = "EXPENSE".equals(transaction.getType()) && score >= 40;
        
        log.info("=== Fraud detection complete: Score = {}, Risk = {}, Fraudulent = {} ===", 
            score, riskLevel, fraudulent);
        
        if (score >= 70) {
            log.warn("HIGH FRAUD SCORE: {} for transaction {}. Reasons: {}", 
                score, transaction.getId(), String.join("; ", reasons));
        } else if (score >= 40) {
            log.info("MEDIUM FRAUD SCORE: {} for transaction {}. Reasons: {}", 
                score, transaction.getId(), String.join("; ", reasons));
        } else if (score > 0) {
            log.info("LOW FRAUD SCORE: {} for transaction {}. Reasons: {}", 
                score, transaction.getId(), String.join("; ", reasons));
        } else {
            log.info("NO FRAUD DETECTED: Score = 0 for transaction {}", transaction.getId());
        }
        
        return FraudDetectionResult.builder()
            .fraudulent(fraudulent)
            .fraudScore(score)
            .riskLevel(riskLevel)
            .reasons(reasons)
            .build();
    }
    
    /**
     * Calculates user's average transaction amount.
     */
    private BigDecimal calculateUserAverage(User user) {
        return transactionRepository.calculateAverageAmount(user);
    }
    
    /**
     * Checks for rapid-fire transactions (5+ within 10 minutes).
     * The transaction being analyzed is already saved in the database.
     */
    private boolean hasRapidFireActivity(User user, LocalDateTime transactionTime) {
        LocalDateTime tenMinutesAgo = transactionTime.minusMinutes(10);
        LocalDateTime tenMinutesAfter = transactionTime.plusMinutes(10);
        
        // Count all transactions in a 10-minute window around this transaction
        long recentCount = transactionRepository.countByUserAndTransactionDateBetween(
            user, tenMinutesAgo, tenMinutesAfter);
        
        log.info("Rapid-fire check: {} transactions in 10-minute window (need 5+)", recentCount);
        
        return recentCount >= 5; // 5 or more transactions in 10-minute window
    }
    
    /**
     * Checks for geographical anomalies (different location within 2 hours).
     */
    private boolean hasGeographicalAnomaly(User user, String location, LocalDateTime transactionTime) {
        List<Transaction> recentTransactions = transactionRepository
            .findByUserOrderByTransactionDateDesc(user);
        
        // Find the most recent transaction before this one that has a location
        for (Transaction prevTxn : recentTransactions) {
            if (prevTxn.getLocation() != null && !prevTxn.getLocation().isBlank()) {
                long hoursBetween = ChronoUnit.HOURS.between(
                    prevTxn.getTransactionDate(), transactionTime);
                
                log.info("Geo check: Previous location = {}, Current location = {}, Hours between = {}", 
                    prevTxn.getLocation(), location, hoursBetween);
                
                boolean differentLocation = !location.equalsIgnoreCase(prevTxn.getLocation());
                boolean withinTwoHours = Math.abs(hoursBetween) < 2;
                
                log.info("Geo check: Different location = {}, Within 2 hours = {}", 
                    differentLocation, withinTwoHours);
                
                return differentLocation && withinTwoHours;
            }
        }
        
        log.info("Geo check: No previous transaction with location found");
        return false;
    }
    
    /**
     * Checks if category is new for user.
     * Since the transaction is already saved, we need to check if this is the ONLY
     * transaction with this category.
     */
    private boolean isUnusualCategory(User user, String category) {
        List<String> userCategories = transactionRepository
            .findDistinctCategoriesByUser(user);
        
        log.info("Category check: User has used {} categories: {}", 
            userCategories.size(), userCategories);
        
        // If this category appears in the list, check if it's only from the current transaction
        if (userCategories.contains(category)) {
            // Count how many transactions have this category
            List<Transaction> allUserTransactions = transactionRepository
                .findByUserOrderByTransactionDateDesc(user);
            
            long categoryCount = allUserTransactions.stream()
                .filter(t -> category.equals(t.getCategory()))
                .count();
            
            log.info("Category check: Category '{}' used {} times", category, categoryCount);
            
            // If only 1 transaction has this category, it's the current one (unusual)
            return categoryCount == 1;
        }
        
        // Category not in list at all (shouldn't happen since transaction is saved)
        log.info("Category check: Category '{}' not in list (unusual)", category);
        return true;
    }
    
    /**
     * Converts fraud score to risk level.
     */
    private RiskLevel calculateRiskLevel(double score) {
        return RiskLevel.fromScore(score);
    }
}
