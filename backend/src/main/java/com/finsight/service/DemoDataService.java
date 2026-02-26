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

            // Create fraud alert only for MEDIUM and HIGH severity (score >= 40)
            if (result.getFraudScore() >= 40 && !result.getReasons().isEmpty()) {
                createFraudAlert(txn, result);
                fraudAlertCount++;
                log.info("Created fraud alert for demo transaction {} with score {} ({})",
                    txn.getId(), result.getFraudScore(), result.getRiskLevel());
            }
        }

        log.info("Generated {} demo transactions ({} fraud alerts) for user {}",
            demoTransactions.size(), fraudAlertCount, user.getId());

        return demoTransactions.size();
    }

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

    private List<Transaction> generateDemoTransactions(User user, Random random) {
        List<Transaction> transactions = new ArrayList<>();

        // Generate transactions across 12 months for better trend analysis
        LocalDateTime now = LocalDateTime.now();
        int[] transactionsPerMonth = {12, 14, 15, 16, 18, 20, 22, 24, 25, 26, 28, 30}; // Increasing trend over 12 months

        log.info("Starting demo transaction generation. Current date: {}", now);

        for (int monthIndex = 0; monthIndex < 12; monthIndex++) {
            int monthsBack = 11 - monthIndex; // Go back 11 months from current month
            LocalDateTime monthStart = now.minusMonths(monthsBack)
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

            int daysInMonth = monthStart.toLocalDate().lengthOfMonth();
            int txnCount = transactionsPerMonth[monthIndex];

            log.info("Generating {} transactions for month: {} {}", txnCount, monthStart.getMonth(), monthStart.getYear());
            for (int i = 0; i < txnCount; i++) {
                int dayOfMonth = 1 + random.nextInt(daysInMonth);
                int hour = 8 + random.nextInt(14); // 8 AM - 10 PM
                int minute = random.nextInt(60);

                LocalDateTime txnDate = monthStart
                    .withDayOfMonth(dayOfMonth)
                    .withHour(hour)
                    .withMinute(minute);

                Transaction txn = Transaction.builder()
                    .user(user)
                    .createdAt(txnDate)
                    .build();

                txn.setTransactionDate(txnDate);

                String category = selectRandomCategory(random);
                txn.setCategory(category);
                txn.setAmount(generateAmountForCategory(category, random));
                txn.setType(category.equals("salary") ? "INCOME" : "EXPENSE");
                txn.setDescription("Demo " + category);
                txn.setLocation("Demo Location " + (random.nextInt(3) + 1));

                transactions.add(txn);

                if (i == 0) {
                    log.info("  First transaction date: {} {}", txnDate.getMonth(), txnDate.getYear());
                }
            }
        }

        log.info("Generated {} total transactions across 12 months", transactions.size());

        addFraudTriggers(transactions, user, random);

        return transactions;
    }

    private String selectRandomCategory(Random random) {
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

        int totalTxns = transactions.size();

        // Scenario 1
        if (totalTxns >= 5) {
            int idx = 2;
            Transaction highAmountTxn = transactions.get(idx);
            highAmountTxn.setAmount(BigDecimal.valueOf(5000 + random.nextInt(3001)));
            highAmountTxn.setCategory("luxury_electronics");
            highAmountTxn.setDescription("Demo: Expensive electronics purchase");
            log.debug("Fraud scenario 1 at index {} on date {}", idx, highAmountTxn.getTransactionDate());
        }

        // Scenario 2
        if (totalTxns >= 15) {
            int idx = 12;
            Transaction txn = transactions.get(idx);
            txn.setAmount(BigDecimal.valueOf(6000 + random.nextInt(2001)));
            txn.setCategory("jewelry_luxury");
            txn.setDescription("Demo: Luxury jewelry purchase");
            log.debug("Fraud scenario 2 at index {} on date {}", idx, txn.getTransactionDate());
        }

        // Scenario 3
        if (totalTxns >= 30) {
            int idx = 25;
            Transaction fraudTxn = transactions.get(idx);
            fraudTxn.setAmount(BigDecimal.valueOf(9000 + random.nextInt(3001)));
            fraudTxn.setCategory("crypto_exchange");
            fraudTxn.setDescription("Demo: Large crypto exchange transaction");
            fraudTxn.setLocation("Foreign Location");
            log.debug("Fraud scenario 3 at index {} on date {}", idx, fraudTxn.getTransactionDate());
        }

        // Scenario 4
        if (totalTxns >= 35) {
            int idx1 = 28;
            int idx2 = 29;
            Transaction txn1 = transactions.get(idx1);
            txn1.setAmount(BigDecimal.valueOf(7500));
            txn1.setCategory("offshore_wire");
            txn1.setDescription("Demo: Offshore wire transfer");
            txn1.setLocation("International");

            Transaction txn2 = transactions.get(idx2);
            txn2.setAmount(BigDecimal.valueOf(8200));
            txn2.setCategory("precious_metals");
            txn2.setDescription("Demo: Precious metals purchase");
            txn2.setLocation("International");

            log.debug("Fraud scenario 4 at indices {},{} on dates {}, {}", 
                idx1, idx2, txn1.getTransactionDate(), txn2.getTransactionDate());
        }

        // Scenario 5
        if (totalTxns >= 45) {
            int idx1 = 42;
            int idx2 = 44;
            int idx3 = 46;

            if (idx3 < totalTxns) {
                Transaction txn1 = transactions.get(idx1);
                txn1.setAmount(BigDecimal.valueOf(10000 + random.nextInt(5001)));
                txn1.setCategory("art_collectibles");
                txn1.setDescription("Demo: High-value art purchase");
                txn1.setLocation("Auction House");

                Transaction txn2 = transactions.get(idx2);
                txn2.setAmount(BigDecimal.valueOf(12000 + random.nextInt(3001)));
                txn2.setCategory("luxury_vehicle_deposit");
                txn2.setDescription("Demo: Luxury vehicle deposit");
                txn2.setLocation("Dealership");

                Transaction txn3 = transactions.get(idx3);
                txn3.setAmount(BigDecimal.valueOf(15000 + random.nextInt(5001)));
                txn3.setCategory("investment_offshore");
                txn3.setDescription("Demo: Offshore investment");
                txn3.setLocation("Foreign Bank");

                log.debug("Fraud scenario 5 at indices {},{},{} on dates {}, {}, {}", 
                    idx1, idx2, idx3, txn1.getTransactionDate(), txn2.getTransactionDate(), txn3.getTransactionDate());
            }
        }
    }
}