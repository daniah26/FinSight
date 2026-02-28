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
import java.time.YearMonth;
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
     * Maximum allowed variance between transaction amounts to still be considered
     * a fixed-price subscription. 1% handles minor rounding/currency differences
     * while rejecting variable-spend categories like groceries.
     */
    private static final double AMOUNT_TOLERANCE_PERCENT = 1.0;

    /**
     * Detects subscriptions from user's transaction history.
     *
     * Strict rules — ALL must pass:
     *   1. EXPENSE transactions grouped by category (case-insensitive).
     *   2. Exactly ONE transaction per month in every month it appears
     *      (categories with 2+ transactions in any month are rejected — e.g. groceries).
     *   3. Transactions appear in at least 2 consecutive calendar months.
     *   4. All transaction amounts are identical (within 1% tolerance).
     *      Variable-spend categories (electricity bills, groceries) are rejected.
     *
     * @param userId The user to analyze
     * @return List of detected/updated subscriptions
     */
    @Transactional
    public List<Subscription> detectSubscriptions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Preserve existing subscription statuses (especially IGNORED)
        List<Subscription> existingSubs = subscriptionRepository.findByUser(user);
        Map<String, Subscription> existingByCategory = existingSubs.stream()
                .collect(Collectors.toMap(
                        s -> s.getMerchant().toLowerCase().trim(),
                        s -> s,
                        (s1, s2) -> s1
                ));

        // Fetch ALL transactions — case-insensitive type filter handles
        // any casing variation ("EXPENSE", "expense", "Expense")
        List<Transaction> allTxns = transactionRepository.findByUserOrderByTransactionDateDesc(user);
        List<Transaction> expenses = allTxns.stream()
                .filter(t -> t.getType() != null && t.getType().equalsIgnoreCase("EXPENSE"))
                .filter(t -> t.getCategory() != null && !t.getCategory().isBlank())
                .collect(Collectors.toList());

        log.info("User {}: {} total transactions, {} EXPENSE transactions found",
                userId, allTxns.size(), expenses.size());

        // Group by normalized (lowercase) category
        Map<String, List<Transaction>> byCategory = expenses.stream()
                .collect(Collectors.groupingBy(t -> t.getCategory().toLowerCase().trim()));

        List<Subscription> detectedSubscriptions = new ArrayList<>();

        for (Map.Entry<String, List<Transaction>> entry : byCategory.entrySet()) {
            String normalizedCategory = entry.getKey();
            List<Transaction> txns = entry.getValue();

            // Sort ascending by date
            txns.sort(Comparator.comparing(Transaction::getTransactionDate));

            // Group by calendar month
            Map<YearMonth, List<Transaction>> byMonth = txns.stream()
                    .collect(Collectors.groupingBy(t ->
                            YearMonth.from(t.getTransactionDate().toLocalDate())));

            // ── Rule 1: Exactly ONE transaction per month ──────────────────────
            // If any month has more than 1 transaction, this is not a subscription
            // (e.g. groceries bought 3x a month, or utility bills with adjustments)
            boolean multipleInAnyMonth = byMonth.values().stream()
                    .anyMatch(monthTxns -> monthTxns.size() > 1);

            if (multipleInAnyMonth) {
                log.info("Category '{}': multiple transactions in a single month — not a subscription (e.g. groceries)",
                        normalizedCategory);
                continue;
            }

            // From here on, exactly one transaction per month — use those directly
            List<YearMonth> months = byMonth.keySet().stream()
                    .sorted()
                    .collect(Collectors.toList());

            log.info("Category '{}': 1 transaction/month in months: {}", normalizedCategory, months);

            // ── Rule 2: At least 2 consecutive calendar months ─────────────────
            boolean hasConsecutiveMonths = false;
            for (int i = 1; i < months.size(); i++) {
                if (ChronoUnit.MONTHS.between(months.get(i - 1), months.get(i)) == 1) {
                    hasConsecutiveMonths = true;
                    break;
                }
            }

            if (!hasConsecutiveMonths) {
                log.info("Category '{}': no consecutive monthly pattern — skipping", normalizedCategory);
                continue;
            }

            // ── Rule 3: Fixed amount (all transactions within 1% of each other) ─
            // Real subscriptions (Netflix, Spotify, gym) charge the exact same amount.
            // Variable spend (groceries, electricity) will fail this check.
            BigDecimal minAmount = txns.stream().map(Transaction::getAmount).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal maxAmount = txns.stream().map(Transaction::getAmount).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            if (minAmount.compareTo(BigDecimal.ZERO) > 0) {
                double variancePercent = maxAmount.subtract(minAmount)
                        .divide(minAmount, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();

                if (variancePercent > AMOUNT_TOLERANCE_PERCENT) {
                    log.info("Category '{}': amount varies {:.2f}% (min={}, max={}) — not a fixed subscription",
                            normalizedCategory, variancePercent, minAmount, maxAmount);
                    continue;
                }
            }

            log.info("Category '{}': all rules passed ✓ — detected as subscription", normalizedCategory);

            // Use the single representative transaction per month for amounts
            // (since amounts are fixed, just use the first transaction's amount)
            BigDecimal fixedAmount = txns.get(0).getAmount();

            Transaction lastTxn = txns.get(txns.size() - 1);
            LocalDate lastPaidDate = lastTxn.getTransactionDate().toLocalDate();
            LocalDate nextDueDate = lastPaidDate.plusDays(30);

            String displayCategory = txns.get(0).getCategory();

            Subscription existing = existingByCategory.get(normalizedCategory);
            if (existing != null) {
                existing.setAvgAmount(fixedAmount);
                existing.setLastPaidDate(lastPaidDate);
                existing.setNextDueDate(nextDueDate);
                // Do NOT touch status — preserves IGNORED
                detectedSubscriptions.add(existing);
                log.info("Updated '{}': amount={}, nextDue={}, status={}",
                        displayCategory, fixedAmount, nextDueDate, existing.getStatus());
            } else {
                Subscription sub = Subscription.builder()
                        .user(user)
                        .merchant(displayCategory)
                        .avgAmount(fixedAmount)
                        .lastPaidDate(lastPaidDate)
                        .nextDueDate(nextDueDate)
                        .status(SubscriptionStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .build();
                detectedSubscriptions.add(sub);
                log.info("New subscription '{}': amount={}, nextDue={}", displayCategory, fixedAmount, nextDueDate);
            }
        }

        log.info("User {}: {} subscription(s) detected/updated", userId, detectedSubscriptions.size());
        return subscriptionRepository.saveAll(detectedSubscriptions);
    }

    /**
     * Finds ACTIVE subscriptions due within the specified number of days.
     */
    public List<Subscription> findDueSoon(Long userId, int days) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(days);

        return subscriptionRepository.findDueSoon(user, start, end);
    }
}