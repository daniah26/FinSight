package com.finsight.service;

import com.finsight.dto.FraudDetectionResult;
import com.finsight.model.FraudAlert;
import com.finsight.model.Transaction;
import com.finsight.model.User;
import com.finsight.repository.FraudAlertRepository;
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
    private final FraudAlertRepository fraudAlertRepository;
    
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
        
        return seedUser(user);
    }
    
    /**
     * Forces demo data reseed by deleting all existing transactions and creating new ones.
     * 
     * @param userId The user to reseed data for
     * @return Number of transactions created
     */
    @Transactional
    public int forceReseedUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // Delete all existing fraud alerts first (foreign key constraint)
        List<FraudAlert> existingAlerts = fraudAlertRepository.findByUserOrderByCreatedAtDesc(user);
        if (!existingAlerts.isEmpty()) {
            fraudAlertRepository.deleteAll(existingAlerts);
            log.info("Deleted {} existing fraud alerts for user {}", existingAlerts.size(), userId);
        }
        
        // Delete all existing transactions
        List<Transaction> existingTransactions = transactionRepository.findByUserOrderByTransactionDateDesc(user);
        if (!existingTransactions.isEmpty()) {
            transactionRepository.deleteAll(existingTransactions);
            log.info("Deleted {} existing transactions for user {}", existingTransactions.size(), userId);
        }
        
        return seedUser(user);
    }
    
    /**
     * Internal method to seed demo transactions for a user.
     * 
     * @param user The user entity
     * @return Number of transactions created
     */
    private int seedUser(User user) {
        // Deterministic seed based on userId
        Random random = new Random(user.getId().hashCode());
        
        List<Transaction> demoTransactions = generateDemoTransactions(user, random);
        
        int fraudAlertCount = 0;
        
        // Process each transaction individually: save, detect fraud, create alert
        for (Transaction txn : demoTransactions) {
            // Save transaction FIRST (required for fraud detection to work)
            txn = transactionRepository.save(txn);
            
            // Run fraud detection AFTER saving
            FraudDetectionResult result = fraudDetectionService.analyzeTransaction(txn);
            
            // Update transaction with fraud results
            txn.setFraudulent(result.isFraudulent());
            txn.setFraudScore(result.getFraudScore());
            txn = transactionRepository.save(txn);
            
            // Create fraud alert if ANY rule triggered (score > 0)
            if (result.getFraudScore() > 0 && !result.getReasons().isEmpty()) {
                createFraudAlert(txn, result);
                fraudAlertCount++;
                log.info("Created fraud alert for demo transaction {} with score {} ({})", 
                    txn.getId(), result.getFraudScore(), result.getRiskLevel());
            }
        }
        
        auditLogService.logAction(user.getId(), "SEED_DEMO_DATA", "TRANSACTION", 
            null, String.format("{\"count\": %d, \"fraudAlerts\": %d}", demoTransactions.size(), fraudAlertCount));
        
        log.info("Generated {} demo transactions ({} fraud alerts) for user {}", 
            demoTransactions.size(), fraudAlertCount, user.getId());
        
        return demoTransactions.size();
    }
    
    /**
     * Creates a fraud alert for a transaction.
     */
    private void createFraudAlert(Transaction transaction, FraudDetectionResult fraudResult) {
        FraudAlert alert = FraudAlert.builder()
            .user(transaction.getUser())
            .transaction(transaction)
            .message(String.format("Suspicious transaction detected: %s", 
                String.join(", ", fraudResult.getReasons())))
            .severity(fraudResult.getRiskLevel())
            .resolved(false)
            .createdAt(LocalDateTime.now())
            .build();
        
        fraudAlertRepository.save(alert);
        log.warn("Created fraud alert for transaction {} with severity {}", 
            transaction.getId(), fraudResult.getRiskLevel());
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
        int count = 35 + random.nextInt(16); // 35-50 (increased to ensure fraud scenarios)
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
    
    /**
     * Adds fraud trigger scenarios to demo transactions.
     * 
     * Fraud Detection Rules:
     * - High amount (>3x average): +30 points
     * - Rapid-fire (5+ in 10 min): +25 points
     * - Geographical anomaly (different location < 2 hours): +25 points
     * - Unusual category (first time): +20 points
     * - Score >= 70 = FRAUDULENT
     * 
     * Scenarios created:
     * 1. High amount + unusual category = 50 points (MEDIUM risk)
     * 2. Rapid-fire cluster = 25 points (LOW risk)
     * 3. High amount + rapid-fire + unusual category = 75 points (HIGH FRAUD)
     * 4. Geographical anomaly + high amount = 55 points (MEDIUM risk)
     * 5. All rules triggered = 100 points (EXTREME FRAUD)
     */
    private void addFraudTriggers(List<Transaction> transactions, User user, Random random) {
        if (transactions.isEmpty()) return;
        
        // Scenario 1: High amount + unusual category (30 + 20 = 50 points - MEDIUM risk)
        if (transactions.size() >= 3) {
            Transaction highAmountTxn = transactions.get(random.nextInt(transactions.size()));
            highAmountTxn.setAmount(BigDecimal.valueOf(5000 + random.nextInt(5001))); // Very high amount
            highAmountTxn.setCategory("luxury_electronics");
            highAmountTxn.setDescription("Demo: Expensive electronics purchase");
            highAmountTxn.setTransactionDate(LocalDateTime.now().minusDays(5));
        }
        
        // Scenario 2: Rapid-fire cluster (5 transactions in 10 minutes = 25 points - LOW risk)
        if (transactions.size() >= 10) {
            LocalDateTime clusterTime = LocalDateTime.now().minusDays(10);
            for (int i = 0; i < 5; i++) {
                transactions.get(i).setTransactionDate(clusterTime.plusMinutes(i * 2));
                transactions.get(i).setDescription("Demo: Rapid transaction " + (i + 1));
                transactions.get(i).setAmount(BigDecimal.valueOf(50 + random.nextInt(100)));
            }
        }
        
        // Scenario 3: HIGH FRAUD - High amount + rapid-fire + unusual category (30 + 25 + 20 = 75 points)
        if (transactions.size() >= 15) {
            LocalDateTime fraudTime = LocalDateTime.now().minusDays(3);
            
            // Create 5 transactions in rapid succession with high amounts and unusual categories
            for (int i = 0; i < 5; i++) {
                int idx = 10 + i;
                if (idx < transactions.size()) {
                    Transaction fraudTxn = transactions.get(idx);
                    fraudTxn.setTransactionDate(fraudTime.plusMinutes(i));
                    fraudTxn.setAmount(BigDecimal.valueOf(3000 + random.nextInt(2001))); // High amounts
                    fraudTxn.setCategory("crypto_exchange_" + i);
                    fraudTxn.setDescription("Demo: Suspicious crypto transaction " + (i + 1));
                    fraudTxn.setLocation("Foreign Location " + (i + 1));
                }
            }
        }
        
        // Scenario 4: Geographical anomaly + high amount (25 + 30 = 55 points - MEDIUM risk)
        if (transactions.size() >= 20) {
            LocalDateTime geoTime = LocalDateTime.now().minusDays(7);
            
            // First transaction in Location A
            Transaction txn1 = transactions.get(15);
            txn1.setTransactionDate(geoTime);
            txn1.setLocation("New York");
            txn1.setAmount(BigDecimal.valueOf(100));
            txn1.setDescription("Demo: Purchase in New York");
            
            // Second transaction in Location B within 1 hour (impossible travel)
            Transaction txn2 = transactions.get(16);
            txn2.setTransactionDate(geoTime.plusMinutes(30));
            txn2.setLocation("Los Angeles");
            txn2.setAmount(BigDecimal.valueOf(4000)); // High amount
            txn2.setDescription("Demo: Suspicious purchase in Los Angeles");
        }
        
        // Scenario 5: EXTREME FRAUD - All rules triggered (30 + 25 + 25 + 20 = 100 points)
        if (transactions.size() >= 25) {
            LocalDateTime extremeTime = LocalDateTime.now().minusDays(1);
            
            // Setup: Normal transaction in Location A
            Transaction setup = transactions.get(20);
            setup.setTransactionDate(extremeTime.minusHours(3));
            setup.setLocation("Chicago");
            setup.setAmount(BigDecimal.valueOf(50));
            setup.setDescription("Demo: Normal purchase");
            
            // Create 5 rapid transactions in different location with high amounts and unusual categories
            for (int i = 0; i < 5; i++) {
                int idx = 21 + i;
                if (idx < transactions.size()) {
                    Transaction extremeTxn = transactions.get(idx);
                    extremeTxn.setTransactionDate(extremeTime.plusMinutes(i));
                    extremeTxn.setLocation("Tokyo"); // Different location within 2 hours
                    extremeTxn.setAmount(BigDecimal.valueOf(8000 + random.nextInt(2001))); // Very high
                    extremeTxn.setCategory("offshore_transfer_" + i); // Unusual category
                    extremeTxn.setDescription("Demo: EXTREME FRAUD - Offshore transfer " + (i + 1));
                }
            }
        }
    }
}
