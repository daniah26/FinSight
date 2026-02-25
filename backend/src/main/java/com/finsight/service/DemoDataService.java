package com.finsight.service;

import com.finsight.dto.FraudDetectionResult;
import com.finsight.model.Transaction;
import com.finsight.model.User;
import com.finsight.repository.TransactionRepository;
import com.finsight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoDataService {
    
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final FraudDetectionService fraudDetectionService;
    private final AuditLogService auditLogService;
    
    private static final String[] CATEGORIES = {
        "groceries", "utilities", "entertainment", "transport", "subscriptions", "salary", "rent"
    };
    
    /**
     * Seeds demo transactions for a user if they have zero transactions.
     * Uses deterministic random generation based on userId.
     * 
     * @param userId The user to seed data for
     * @return Number of transactions created
     */
    @Transactional
    public int seedUserIfEmpty(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        Long count = transactionRepository.countByUser(user);
        
        if (count > 0) {
            log.info("User {} already has {} transactions, skipping demo data seeding", userId, count);
            return 0;
        }
        
        // Deterministic seed based on userId
        Random random = new Random(userId.hashCode());
        
        List<Transaction> demoTransactions = generateDemoTransactions(user, random);
        
        // Run fraud detection on each
        for (Transaction txn : demoTransactions) {
            FraudDetectionResult result = fraudDetectionService.analyzeTransaction(txn);
            txn.setFraudulent(result.isFraudulent());
            txn.setFraudScore(result.getFraudScore());
        }
        
        transactionRepository.saveAll(demoTransactions);
        
        auditLogService.logAction(userId, "SEED_DEMO_DATA", "TRANSACTION", 
            null, String.format("{\"count\": %d}", demoTransactions.size()));
        
        log.info("Generated {} demo transactions for user {}", demoTransactions.size(), userId);
        
        return demoTransactions.size();
    }
    
    /**
     * Generates a deterministic set of demo transactions.
     * 
     * @param user The user entity
     * @param random Seeded random generator
     * @return List of generated transactions
     */
    private List<Transaction> generateDemoTransactions(User user, Random random) {
        List<Transaction> transactions = new ArrayList<>();
        int count = 25 + random.nextInt(26); // 25-50
        int daysBack = 60 + random.nextInt(31); // 60-90
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(daysBack);
        
        for (int i = 0; i < count; i++) {
            Transaction txn = Transaction.builder()
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
            
            // Random date within range
            long randomDays = random.nextInt(daysBack);
            txn.setTransactionDate(startDate.plusDays(randomDays));
            
            // Random category and amount
            String category = selectRandomCategory(random);
            txn.setCategory(category);
            txn.setAmount(generateAmountForCategory(category, random));
            txn.setType(category.equals("salary") ? "INCOME" : "EXPENSE");
            
            txn.setDescription("Demo " + category);
            txn.setLocation("Demo Location " + (random.nextInt(3) + 1));
            
            transactions.add(txn);
        }
        
        // Add fraud triggers
        addFraudTriggers(transactions, user, random);
        
        return transactions;
    }
    
    private String selectRandomCategory(Random random) {
        // Weight distribution: 40% groceries, 15% utilities, 15% entertainment, 
        // 10% transport, 10% subscriptions, 5% salary, 5% rent
        int roll = random.nextInt(100);
        if (roll < 40) return "groceries";
        if (roll < 55) return "utilities";
        if (roll < 70) return "entertainment";
        if (roll < 80) return "transport";
        if (roll < 90) return "subscriptions";
        if (roll < 95) return "salary";
        return "rent";
    }
    
    private BigDecimal generateAmountForCategory(String category, Random random) {
        return switch (category) {
            case "groceries" -> BigDecimal.valueOf(20 + random.nextInt(131)); // 20-150
            case "utilities" -> BigDecimal.valueOf(50 + random.nextInt(251)); // 50-300
            case "entertainment" -> BigDecimal.valueOf(10 + random.nextInt(91)); // 10-100
            case "transport" -> BigDecimal.valueOf(10 + random.nextInt(71)); // 10-80
            case "subscriptions" -> BigDecimal.valueOf(5 + random.nextInt(46)); // 5-50
            case "salary" -> BigDecimal.valueOf(2000 + random.nextInt(3001)); // 2000-5000
            case "rent" -> BigDecimal.valueOf(800 + random.nextInt(1201)); // 800-2000
            default -> BigDecimal.valueOf(50 + random.nextInt(101)); // 50-150
        };
    }
    
    private void addFraudTriggers(List<Transaction> transactions, User user, Random random) {
        if (transactions.isEmpty()) return;
        
        // Add 1-2 high amount transactions (>3x average)
        if (transactions.size() >= 3) {
            Transaction highAmountTxn = transactions.get(random.nextInt(transactions.size()));
            highAmountTxn.setAmount(BigDecimal.valueOf(5000 + random.nextInt(5001))); // Very high amount
            highAmountTxn.setCategory("unusual_purchase");
            highAmountTxn.setDescription("Demo high amount transaction");
        }
        
        // Add cluster of 5+ transactions within 10 minutes for rapid-fire detection
        if (transactions.size() >= 5) {
            LocalDateTime clusterTime = LocalDateTime.now().minusDays(random.nextInt(30));
            for (int i = 0; i < 5; i++) {
                if (i < transactions.size()) {
                    transactions.get(i).setTransactionDate(clusterTime.plusMinutes(i));
                }
            }
        }
        
        // Add unusual category transaction
        if (transactions.size() >= 2) {
            Transaction unusualTxn = transactions.get(transactions.size() - 1);
            unusualTxn.setCategory("rare_category_" + random.nextInt(1000));
            unusualTxn.setDescription("Demo unusual category");
        }
    }
}
