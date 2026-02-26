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
     * Groups by merchant, finds recurring patterns (20-40 days apart).
     * Merges with existing subscriptions to preserve status (IGNORED).
     * 
     * @param userId The user to analyze
     * @return List of detected subscriptions
     */
    @Transactional
    public List<Subscription> detectSubscriptions(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // Get existing subscriptions to preserve their status
        List<Subscription> existingSubs = subscriptionRepository.findByUser(user);
        Map<String, Subscription> existingByMerchant = existingSubs.stream()
            .collect(Collectors.toMap(
                s -> normalizeMerchant(s.getMerchant()),
                s -> s,
                (s1, s2) -> s1 // Keep first if duplicates
            ));
        
        List<Transaction> expenses = transactionRepository.findByUserAndType(user, "EXPENSE");
        
        Map<String, List<Transaction>> byMerchant = groupByMerchant(expenses);
        List<Subscription> subscriptions = new ArrayList<>();
        
        for (Map.Entry<String, List<Transaction>> entry : byMerchant.entrySet()) {
            List<Transaction> txns = entry.getValue();
            if (txns.size() < 2) continue;
            
            txns.sort(Comparator.comparing(Transaction::getTransactionDate));
            
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
            if (filteredTxns.size() < 2) continue;
            
            boolean isRecurring = false;
            int recurringCount = 0;
            
            for (int i = 1; i < filteredTxns.size(); i++) {
                long daysBetween = ChronoUnit.DAYS.between(
                    filteredTxns.get(i-1).getTransactionDate(),
                    filteredTxns.get(i).getTransactionDate()
                );
                // Check for monthly pattern (20-40 days)
                if (daysBetween >= 20 && daysBetween <= 40) {
                    recurringCount++;
                }
            }
            
            // Need at least 1 recurring pattern (2 transactions ~30 days apart)
            if (recurringCount >= 1) {
                String normalizedMerchant = entry.getKey();
                Subscription existingSub = existingByMerchant.get(normalizedMerchant);
                
                if (existingSub != null) {
                    // Update existing subscription with new data but preserve status
                    updateSubscription(existingSub, filteredTxns);
                    subscriptions.add(existingSub);
                    log.info("Updated existing subscription for user {}: merchant={}, status={}", 
                        userId, existingSub.getMerchant(), existingSub.getStatus());
                } else {
                    // Create new subscription
                    Subscription sub = createSubscription(user, entry.getKey(), filteredTxns);
                    subscriptions.add(sub);
                    log.info("Detected new subscription for user {}: merchant={}, avgAmount={}, nextDue={}", 
                        userId, sub.getMerchant(), sub.getAvgAmount(), sub.getNextDueDate());
                }
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
     * For categories, just use lowercase.
     */
    private String normalizeMerchant(String merchant) {
        if (merchant == null) return "unknown";
        return merchant.toLowerCase().trim();
    }
    
    /**
     * Groups transactions by category (for subscription detection).
     */
    private Map<String, List<Transaction>> groupByMerchant(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> t.getCategory() != null)
            .collect(Collectors.groupingBy(Transaction::getCategory));
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
        
        // Update fields but preserve status
        subscription.setAvgAmount(avgAmount);
        subscription.setLastPaidDate(lastPaidDate);
        subscription.setNextDueDate(nextDueDate);
        // Status is preserved - don't change it
    }
}
