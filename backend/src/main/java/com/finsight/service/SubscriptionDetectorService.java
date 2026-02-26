package com.finsight.service;

import com.finsight.model.Subscription;
import com.finsight.model.SubscriptionStatus;
import com.finsight.model.Transaction;
import com.finsight.model.User;
import com.finsight.repository.SubscriptionRepository;
import com.finsight.repository.TransactionRepository;
import com.finsight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionDetectorService {
    
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    
    /**
     * Detects subscriptions from user's transaction history.
     * Groups by category (merchant), finds recurring monthly patterns.
     * 
     * Criteria for subscription detection:
     * - At least 3 transactions with the same category
     * - Only ONE transaction per month for that category (strict monthly pattern)
     * - Transactions must be 25-35 days apart (monthly pattern)
     * - At least 2 consecutive monthly occurrences
     * 
     * This ensures only true recurring monthly subscriptions are detected,
     * not categories where users make multiple purchases per month.
     * 
     * @param userId The user to analyze
     * @return List of detected subscriptions
     */
    @Transactional
    public List<Subscription> detectSubscriptions(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // Get existing subscriptions to update them instead of deleting
        List<Subscription> existingSubs = subscriptionRepository.findByUser(user);
        Map<String, Subscription> existingByMerchant = existingSubs.stream()
            .collect(Collectors.toMap(
                s -> normalizeMerchant(s.getMerchant()),
                s -> s,
                (s1, s2) -> s1 // Keep first if duplicate
            ));
        
        List<Transaction> expenses = transactionRepository.findByUserAndType(user, "EXPENSE");
        
        Map<String, List<Transaction>> byMerchant = groupByMerchant(expenses);
        List<Subscription> subscriptions = new ArrayList<>();
        
        for (Map.Entry<String, List<Transaction>> entry : byMerchant.entrySet()) {
            List<Transaction> txns = entry.getValue();
            // Require at least 3 transactions to establish a pattern
            if (txns.size() < 3) continue;
            
            txns.sort(Comparator.comparing(Transaction::getTransactionDate));
            
            // STRICT CHECK: Ensure only ONE transaction per month for this category
            // Group transactions by year-month
            Map<String, Long> txnsPerMonth = txns.stream()
                .collect(Collectors.groupingBy(
                    t -> t.getTransactionDate().getYear() + "-" + 
                         String.format("%02d", t.getTransactionDate().getMonthValue()),
                    Collectors.counting()
                ));
            
            // If ANY month has more than 1 transaction for this category, it's NOT a subscription
            boolean hasMultipleInAnyMonth = txnsPerMonth.values().stream()
                .anyMatch(count -> count > 1);
            
            if (hasMultipleInAnyMonth) {
                log.debug("Category '{}' has multiple transactions in at least one month - not a subscription", 
                    entry.getKey());
                continue;
            }
            
            // Filter out transactions that are too close together (less than 20 days)
            // This prevents multiple payments in the same month from being counted separately
            List<Transaction> filteredTxns = new ArrayList<>();
            filteredTxns.add(txns.get(0)); // Always add the first transaction
            
            for (int i = 1; i < txns.size(); i++) {
                long daysSinceLast = ChronoUnit.DAYS.between(
                    filteredTxns.get(filteredTxns.size() - 1).getTransactionDate(),
                    txns.get(i).getTransactionDate()
                );
                
                // Only include if it's at least 20 days since the last included transaction
                if (daysSinceLast >= 20) {
                    filteredTxns.add(txns.get(i));
                }
            }
            
            // Now check if the filtered transactions show a recurring pattern
            // Need at least 3 transactions to confirm it's truly recurring
            if (filteredTxns.size() < 3) continue;
            
            int recurringCount = 0;
            
            for (int i = 1; i < filteredTxns.size(); i++) {
                long daysBetween = ChronoUnit.DAYS.between(
                    filteredTxns.get(i-1).getTransactionDate(),
                    filteredTxns.get(i).getTransactionDate()
                );
                // Check for monthly pattern (25-35 days for stricter matching)
                if (daysBetween >= 25 && daysBetween <= 35) {
                    recurringCount++;
                }
            }
            
            // Need at least 2 recurring patterns (3 transactions ~30 days apart each)
            // This ensures it's truly a monthly subscription, not just coincidence
            if (recurringCount >= 2) {
                String normalizedMerchant = entry.getKey();
                
                // Check if subscription already exists for this merchant
                Subscription sub = existingByMerchant.get(normalizedMerchant);
                if (sub != null) {
                    // Update existing subscription
                    updateSubscription(sub, filteredTxns);
                    log.info("Updated subscription for user {}: merchant={}, avgAmount={}, nextDue={}", 
                        userId, sub.getMerchant(), sub.getAvgAmount(), sub.getNextDueDate());
                } else {
                    // Create new subscription
                    sub = createSubscription(user, normalizedMerchant, filteredTxns);
                    log.info("Created new subscription for user {}: merchant={}, avgAmount={}, nextDue={}, transactionCount={}", 
                        userId, sub.getMerchant(), sub.getAvgAmount(), sub.getNextDueDate(), filteredTxns.size());
                }
                subscriptions.add(sub);
            }
        }
        
        return subscriptionRepository.saveAll(subscriptions);
    }
    
    /**
     * Finds subscriptions due within specified days.
     * 
     * @param userId The user
     * @param days Number of days to look ahead
     * @return List of due-soon subscriptions
     */
    public List<Subscription> findDueSoon(Long userId, int days) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(days);
        
        return subscriptionRepository.findDueSoon(user, start, end);
    }
    
    /**
     * Normalizes merchant name for matching.
     * Uses the category field which is more reliable than description.
     */
    private String normalizeMerchant(String merchant) {
        if (merchant == null) return "unknown";
        
        // Simply lowercase and trim - don't extract first word
        // This ensures "Netflix Premium" and "Spotify Premium" are different
        return merchant.toLowerCase().trim();
    }
    
    /**
     * Groups transactions by category (merchant).
     */
    private Map<String, List<Transaction>> groupByMerchant(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> t.getCategory() != null)
            .collect(Collectors.groupingBy(
                t -> normalizeMerchant(t.getCategory())
            ));
    }
    
    private Subscription createSubscription(User user, String normalizedMerchant, List<Transaction> txns) {
        // Calculate average amount
        BigDecimal avgAmount = txns.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(txns.size()), 2, RoundingMode.HALF_UP);
        
        // Get last payment date
        Transaction lastTxn = txns.get(txns.size() - 1);
        LocalDate lastPaidDate = lastTxn.getTransactionDate().toLocalDate();
        
        // Calculate next due date (last + 30 days)
        LocalDate nextDueDate = lastPaidDate.plusDays(30);
        
        return Subscription.builder()
            .user(user)
            .merchant(txns.get(0).getCategory()) // Use category as merchant name
            .avgAmount(avgAmount)
            .lastPaidDate(lastPaidDate)
            .nextDueDate(nextDueDate)
            .status(SubscriptionStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    private void updateSubscription(Subscription subscription, List<Transaction> txns) {
        // Recalculate average amount
        BigDecimal avgAmount = txns.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(txns.size()), 2, RoundingMode.HALF_UP);
        
        // Get last payment date
        Transaction lastTxn = txns.get(txns.size() - 1);
        LocalDate lastPaidDate = lastTxn.getTransactionDate().toLocalDate();
        
        // Calculate next due date (last + 30 days)
        LocalDate nextDueDate = lastPaidDate.plusDays(30);
        
        // Update the subscription
        subscription.setAvgAmount(avgAmount);
        subscription.setLastPaidDate(lastPaidDate);
        subscription.setNextDueDate(nextDueDate);
        // Keep existing status unless it was ignored
        if (subscription.getStatus() != SubscriptionStatus.IGNORED) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }
    }
}
