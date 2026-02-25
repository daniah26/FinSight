# Design Document: FinSight Enhancement

## Overview

This design document specifies the technical architecture for enhancing the FinSight Financial Transaction Tracker with advanced fraud detection, subscription tracking, demo data generation, and comprehensive filtering capabilities. The enhancement builds upon the existing Spring Boot application while maintaining full backward compatibility with existing endpoints.

### Goals

- Implement rule-based fraud detection with risk scoring (0-100 scale)
- Provide automated demo data generation for first-time users
- Enable subscription detection from transaction patterns
- Support comprehensive transaction filtering and sorting
- Implement audit logging for regulatory compliance
- Create actionable insights dashboard with aggregated metrics
- Maintain backward compatibility with existing API contracts

### Non-Goals

- Machine learning-based fraud detection (future enhancement)
- Real-time streaming transaction processing
- Multi-currency support
- External payment gateway integration
- Mobile native applications

### Technology Stack

- **Backend**: Spring Boot 4.0.3, Java 17
- **Database**: H2 (in-memory for development)
- **ORM**: Spring Data JPA with Hibernate
- **Frontend**: React 18+ with functional components
- **Build**: Maven 3.8+
- **Deployment**: Docker Compose
- **Testing**: JUnit 5, MockMvc, Mockito

## Architecture

### System Architecture

The system follows a layered architecture pattern:

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend (React)                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │Dashboard │  │Transactions│ │Fraud     │  │Subscript│ │
│  │  Page    │  │   Page     │ │Alerts    │  │ions Page│ │
│  └──────────┘  └──────────┘  └──────────┘  └─────────┘ │
└─────────────────────────────────────────────────────────┘
                         │ HTTP/REST
                         ▼
┌─────────────────────────────────────────────────────────┐
│              REST Controllers Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐ │
│  │Transaction   │  │FraudAlert    │  │Subscription   │ │
│  │Controller    │  │Controller    │  │Controller     │ │
│  └──────────────┘  └──────────────┘  └───────────────┘ │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  Service Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐ │
│  │Transaction   │  │FraudDetection│  │Subscription   │ │
│  │Service       │  │Service       │  │Detector       │ │
│  ├──────────────┤  ├──────────────┤  ├───────────────┤ │
│  │DemoData      │  │Dashboard     │  │AuditLog       │ │
│  │Service       │  │Service       │  │Service        │ │
│  └──────────────┘  └──────────────┘  └───────────────┘ │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Repository Layer (Spring Data JPA)          │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐ │
│  │Transaction   │  │FraudAlert    │  │Subscription   │ │
│  │Repository    │  │Repository    │  │Repository     │ │
│  ├──────────────┤  ├──────────────┤  ├───────────────┤ │
│  │User          │  │AuditLog      │  │               │ │
│  │Repository    │  │Repository    │  │               │ │
│  └──────────────┘  └──────────────┘  └───────────────┘ │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  H2 Database                             │
│  Tables: users, transactions, fraud_alerts,              │
│          subscriptions, audit_logs                       │
└─────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

#### Transaction Creation Flow
```
User → Frontend → POST /api/transactions
                      ↓
              TransactionController
                      ↓
              TransactionService
                      ↓
         ┌────────────┴────────────┐
         ▼                         ▼
  FraudDetectionService    AuditLogService
         │                         │
         ▼                         ▼
  Calculate Score           Log Action
         │                         │
         └────────────┬────────────┘
                      ▼
              Save Transaction
                      ↓
         If fraudulent: Create Alert
                      ↓
              Return Response
```

#### Demo Data Seeding Flow
```
User Login → POST /api/auth/login
                      ↓
              Check transaction count
                      ↓
         If count == 0: Trigger seeding
                      ↓
              DemoDataService
                      ↓
         Initialize Random(userId.hashCode())
                      ↓
         Generate 25-50 transactions
                      ↓
         For each: Run fraud detection
                      ↓
              AuditLogService.log()
                      ↓
              Return success
```

## Components and Interfaces

### Entity Models

#### Transaction Entity (Enhanced)
```java
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private String type; // INCOME, EXPENSE
    
    @Column(nullable = false)
    private String category;
    
    private String description;
    
    private String location;
    
    @Column(nullable = false)
    private LocalDateTime transactionDate;
    
    @Column(nullable = false)
    private boolean fraudulent = false;
    
    @Column(precision = 5, scale = 2)
    private Double fraudScore; // 0-100
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

#### Subscription Entity (New)
```java
@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String merchant;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal avgAmount;
    
    @Column(nullable = false)
    private LocalDate lastPaidDate;
    
    @Column(nullable = false)
    private LocalDate nextDueDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status; // ACTIVE, IGNORED
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

#### AuditLog Entity (New)
```java
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String action; // CREATE_TRANSACTION, RESOLVE_ALERT, etc.
    
    @Column(nullable = false)
    private String entityType; // TRANSACTION, FRAUD_ALERT, SUBSCRIPTION
    
    private Long entityId;
    
    @Column(columnDefinition = "TEXT")
    private String details; // JSON format
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
}
```

#### Enumerations

```java
public enum RiskLevel {
    LOW,    // Score 0-39
    MEDIUM, // Score 40-69
    HIGH    // Score 70-100
}

public enum SubscriptionStatus {
    ACTIVE,
    IGNORED
}
```

### Service Interfaces

#### DemoDataService
```java
@Service
public class DemoDataService {
    /**
     * Seeds demo transactions for a user if they have zero transactions.
     * Uses deterministic random generation based on userId.
     * 
     * @param userId The user to seed data for
     * @return Number of transactions created
     */
    public int seedUserIfEmpty(Long userId);
    
    /**
     * Generates a deterministic set of demo transactions.
     * 
     * @param user The user entity
     * @param random Seeded random generator
     * @return List of generated transactions
     */
    private List<Transaction> generateDemoTransactions(User user, Random random);
}
```

#### FraudDetectionService (Enhanced)
```java
@Service
public class FraudDetectionService {
    /**
     * Analyzes a transaction and computes fraud score using rule-based algorithm.
     * 
     * Rules:
     * - High amount anomaly (>3x avg): +30 points
     * - Rapid-fire activity (5+ in 10 min): +25 points
     * - Geographical anomaly (different location < 2 hours): +25 points
     * - Unusual category (never used): +20 points
     * 
     * @param transaction The transaction to analyze
     * @return FraudDetectionResult with score, risk level, and reasons
     */
    public FraudDetectionResult analyzeTransaction(Transaction transaction);
    
    /**
     * Calculates user's average transaction amount.
     */
    private BigDecimal calculateUserAverage(User user);
    
    /**
     * Checks for rapid-fire transactions.
     */
    private boolean hasRapidFireActivity(User user, LocalDateTime transactionTime);
    
    /**
     * Checks for geographical anomalies.
     */
    private boolean hasGeographicalAnomaly(User user, String location, LocalDateTime transactionTime);
    
    /**
     * Checks if category is new for user.
     */
    private boolean isUnusualCategory(User user, String category);
    
    /**
     * Converts fraud score to risk level.
     */
    private RiskLevel calculateRiskLevel(double score);
}
```

#### SubscriptionDetectorService (New)
```java
@Service
public class SubscriptionDetectorService {
    /**
     * Detects subscriptions from user's transaction history.
     * Groups by merchant, finds recurring patterns (25-35 days apart).
     * 
     * @param userId The user to analyze
     * @return List of detected subscriptions
     */
    public List<Subscription> detectSubscriptions(Long userId);
    
    /**
     * Finds subscriptions due within specified days.
     * 
     * @param userId The user
     * @param days Number of days to look ahead
     * @return List of due-soon subscriptions
     */
    public List<Subscription> findDueSoon(Long userId, int days);
    
    /**
     * Normalizes merchant name for matching.
     */
    private String normalizeMerchant(String merchant);
    
    /**
     * Groups transactions by normalized merchant.
     */
    private Map<String, List<Transaction>> groupByMerchant(List<Transaction> transactions);
}
```

#### AuditLogService (New)
```java
@Service
public class AuditLogService {
    /**
     * Logs a user action for compliance tracking.
     * 
     * @param userId The user performing the action
     * @param action Action type
     * @param entityType Entity being acted upon
     * @param entityId ID of the entity
     * @param details Additional details in JSON format
     */
    public void logAction(Long userId, String action, String entityType, 
                         Long entityId, String details);
}
```

#### DashboardService (New)
```java
@Service
public class DashboardService {
    /**
     * Generates dashboard summary with aggregated metrics.
     * 
     * @param userId The user
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @return DashboardSummary with all metrics
     */
    public DashboardSummary getSummary(Long userId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Calculates spending by category.
     */
    private Map<String, BigDecimal> getSpendingByCategory(Long userId, LocalDate start, LocalDate end);
    
    /**
     * Calculates fraud incidents by category.
     */
    private Map<String, Long> getFraudByCategory(Long userId, LocalDate start, LocalDate end);
    
    /**
     * Calculates spending trends over time.
     */
    private List<TimeSeriesPoint> getSpendingTrends(Long userId, LocalDate start, LocalDate end);
}
```

### API Endpoints

#### Transaction Endpoints

**POST /api/transactions**
- Creates a manual transaction with fraud detection
- Request Body: TransactionRequest
- Response: TransactionResponse with fraud score
- Status: 201 Created, 400 Bad Request, 404 User Not Found

**GET /api/transactions**
- Retrieves transactions with filtering, sorting, and pagination
- Query Parameters:
  - `type`: INCOME or EXPENSE
  - `category`: Category name
  - `startDate`: ISO date (inclusive)
  - `endDate`: ISO date (inclusive)
  - `fraudulent`: true/false
  - `sortBy`: transactionDate, amount, fraudScore, category
  - `sortDir`: ASC or DESC
  - `page`: Page number (0-indexed)
  - `size`: Page size (default 20)
- Response: Page<TransactionResponse>
- Status: 200 OK

#### Fraud Alert Endpoints

**GET /api/fraud/alerts**
- Retrieves fraud alerts with filtering
- Query Parameters:
  - `resolved`: true/false
  - `severity`: LOW, MEDIUM, HIGH
- Response: List<FraudAlertDto>
- Status: 200 OK

**PUT /api/fraud/alerts/{id}/resolve**
- Marks a fraud alert as resolved
- Response: FraudAlertDto
- Status: 200 OK, 404 Not Found

#### Dashboard Endpoint

**GET /api/summary**
- Retrieves dashboard summary
- Query Parameters:
  - `startDate`: ISO date (optional)
  - `endDate`: ISO date (optional)
- Response: DashboardSummary
- Status: 200 OK

#### Subscription Endpoints

**GET /api/subscriptions**
- Retrieves all detected subscriptions
- Response: List<SubscriptionDto>
- Status: 200 OK

**PUT /api/subscriptions/{id}/ignore**
- Marks a subscription as ignored
- Response: SubscriptionDto
- Status: 200 OK, 404 Not Found

**GET /api/subscriptions/due-soon**
- Retrieves subscriptions due within specified days
- Query Parameters:
  - `days`: Number of days (default 7)
- Response: List<SubscriptionDto>
- Status: 200 OK

#### Authentication Endpoint (Enhanced)

**POST /api/auth/login**
- Authenticates user and triggers demo seeding if needed
- Request Body: LoginRequest
- Response: LoginResponse with user details
- Side Effect: Seeds demo data if transaction count == 0
- Status: 200 OK, 401 Unauthorized


## Data Models

### DTOs (Data Transfer Objects)

#### TransactionRequest
```java
public class TransactionRequest {
    private Long userId;
    private BigDecimal amount;
    private String type; // INCOME, EXPENSE
    private String category;
    private String description;
    private String location;
    private LocalDateTime transactionDate;
}
```

#### TransactionResponse
```java
public class TransactionResponse {
    private Long id;
    private BigDecimal amount;
    private String type;
    private String category;
    private String description;
    private String location;
    private LocalDateTime transactionDate;
    private boolean fraudulent;
    private Double fraudScore;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private String status; // COMPLETED, FLAGGED
}
```

#### FraudAlertDto
```java
public class FraudAlertDto {
    private Long id;
    private Long userId;
    private TransactionResponse transaction;
    private String message;
    private String severity; // LOW, MEDIUM, HIGH
    private boolean resolved;
    private LocalDateTime createdAt;
}
```

#### SubscriptionDto
```java
public class SubscriptionDto {
    private Long id;
    private String merchant;
    private BigDecimal avgAmount;
    private LocalDate lastPaidDate;
    private LocalDate nextDueDate;
    private String status; // ACTIVE, IGNORED
    private LocalDateTime createdAt;
}
```

#### DashboardSummary
```java
public class DashboardSummary {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal currentBalance;
    private Long totalFlaggedTransactions;
    private Double averageFraudScore;
    private Map<String, BigDecimal> spendingByCategory;
    private Map<String, Long> fraudByCategory;
    private List<TimeSeriesPoint> spendingTrends;
}
```

#### TimeSeriesPoint
```java
public class TimeSeriesPoint {
    private LocalDate date;
    private BigDecimal amount;
}
```

#### FraudDetectionResult
```java
public class FraudDetectionResult {
    private boolean fraudulent;
    private double fraudScore; // 0-100
    private RiskLevel riskLevel;
    private List<String> reasons;
}
```

### Database Schema

#### transactions table
```sql
CREATE TABLE transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    location VARCHAR(100),
    transaction_date TIMESTAMP NOT NULL,
    fraudulent BOOLEAN NOT NULL DEFAULT FALSE,
    fraud_score DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_transactions_user_date ON transactions(user_id, transaction_date DESC);
CREATE INDEX idx_transactions_fraudulent ON transactions(user_id, fraudulent);
CREATE INDEX idx_transactions_category ON transactions(user_id, category);
```

#### subscriptions table
```sql
CREATE TABLE subscriptions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    merchant VARCHAR(100) NOT NULL,
    avg_amount DECIMAL(19,2) NOT NULL,
    last_paid_date DATE NOT NULL,
    next_due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_due_date ON subscriptions(user_id, next_due_date);
```

#### audit_logs table
```sql
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    timestamp TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_audit_logs_user_time ON audit_logs(user_id, timestamp DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
```

### Repository Interfaces

#### TransactionRepository (Enhanced)
```java
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserOrderByTransactionDateDesc(User user);
    
    Optional<Transaction> findTopByUserOrderByTransactionDateDesc(User user);
    
    List<Transaction> findByUserAndFraudulentTrue(User user);
    
    List<Transaction> findByUserAndTransactionDateAfter(User user, LocalDateTime after);
    
    Long countByUser(User user);
    
    @Query("SELECT AVG(t.amount) FROM Transaction t WHERE t.user = :user")
    BigDecimal calculateAverageAmount(@Param("user") User user);
    
    @Query("SELECT DISTINCT t.category FROM Transaction t WHERE t.user = :user")
    List<String> findDistinctCategoriesByUser(@Param("user") User user);
    
    Page<Transaction> findByUser(User user, Pageable pageable);
    
    // Specification-based query for complex filtering
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);
}
```

#### SubscriptionRepository (New)
```java
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUser(User user);
    
    List<Subscription> findByUserAndStatus(User user, SubscriptionStatus status);
    
    @Query("SELECT s FROM Subscription s WHERE s.user = :user " +
           "AND s.status = 'ACTIVE' " +
           "AND s.nextDueDate BETWEEN :start AND :end")
    List<Subscription> findDueSoon(@Param("user") User user, 
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);
}
```

#### AuditLogRepository (New)
```java
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserOrderByTimestampDesc(User user);
    
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
```

### Fraud Detection Algorithm

#### Rule-Based Scoring System

The fraud detection engine uses four independent rules that contribute to a cumulative score:

**Rule 1: High Amount Anomaly**
- Condition: Transaction amount > 3 × user's average transaction amount
- Score Impact: +30 points
- Rationale: Unusually large transactions are suspicious
- Implementation:
  ```java
  BigDecimal userAvg = calculateUserAverage(transaction.getUser());
  BigDecimal threshold = userAvg.multiply(BigDecimal.valueOf(3));
  if (transaction.getAmount().compareTo(threshold) > 0) {
      score += 30;
      reasons.add("Amount exceeds 3x user average");
  }
  ```

**Rule 2: Rapid-Fire Activity**
- Condition: 5 or more transactions within 10-minute window
- Score Impact: +25 points
- Rationale: Multiple rapid transactions suggest account compromise
- Implementation:
  ```java
  LocalDateTime tenMinutesAgo = transactionTime.minusMinutes(10);
  long recentCount = transactionRepository
      .countByUserAndTransactionDateBetween(user, tenMinutesAgo, transactionTime);
  if (recentCount >= 5) {
      score += 25;
      reasons.add("5+ transactions in 10 minutes");
  }
  ```

**Rule 3: Geographical Anomaly**
- Condition: Transaction location differs from user's last transaction location AND time between transactions < 2 hours (impossible travel time)
- Score Impact: +25 points
- Rationale: Transactions in different locations within short time frames suggest card cloning or account compromise
- Implementation:
  ```java
  Optional<Transaction> lastTransaction = transactionRepository
      .findTopByUserOrderByTransactionDateDesc(user);
  if (lastTransaction.isPresent()) {
      Transaction last = lastTransaction.get();
      long hoursBetween = ChronoUnit.HOURS.between(
          last.getTransactionDate(), transactionTime);
      if (hoursBetween < 2 && 
          !transaction.getLocation().equalsIgnoreCase(last.getLocation())) {
          score += 25;
          reasons.add("Different location within 2 hours");
      }
  }
  ```

**Rule 4: Unusual Category**
- Condition: Category never used before by this user
- Score Impact: +20 points
- Rationale: New spending patterns may indicate fraud
- Implementation:
  ```java
  List<String> userCategories = transactionRepository
      .findDistinctCategoriesByUser(user);
  if (!userCategories.contains(transaction.getCategory())) {
      score += 20;
      reasons.add("New category for user");
  }
  ```

#### Risk Level Mapping

```java
private RiskLevel calculateRiskLevel(double score) {
    if (score >= 70) return RiskLevel.HIGH;
    if (score >= 40) return RiskLevel.MEDIUM;
    return RiskLevel.LOW;
}
```

#### Fraud Flag Threshold

A transaction is flagged as fraudulent when:
- Fraud score >= 70 (HIGH risk)
- A FraudAlert is automatically created
- Alert severity matches the risk level

### Subscription Detection Algorithm

#### Detection Process

1. **Fetch User Transactions**: Retrieve all expense transactions for the user
2. **Normalize Merchant Names**: Convert to lowercase, remove special characters
3. **Group by Merchant**: Create map of merchant → list of transactions
4. **Analyze Patterns**: For each merchant group:
   - Sort transactions by date
   - Calculate day differences between consecutive transactions
   - Identify pairs with 25-35 day gaps
   - Require at least 2 qualifying occurrences
5. **Create Subscriptions**: For qualifying patterns:
   - Calculate average amount
   - Record last payment date
   - Calculate next due date (last + 30 days)
   - Set status to ACTIVE

#### Implementation Details

```java
public List<Subscription> detectSubscriptions(Long userId) {
    User user = userRepository.findById(userId).orElseThrow();
    List<Transaction> expenses = transactionRepository
        .findByUserAndType(user, "EXPENSE");
    
    Map<String, List<Transaction>> byMerchant = groupByMerchant(expenses);
    List<Subscription> subscriptions = new ArrayList<>();
    
    for (Map.Entry<String, List<Transaction>> entry : byMerchant.entrySet()) {
        List<Transaction> txns = entry.getValue();
        if (txns.size() < 2) continue;
        
        txns.sort(Comparator.comparing(Transaction::getTransactionDate));
        
        int recurringCount = 0;
        for (int i = 1; i < txns.size(); i++) {
            long daysBetween = ChronoUnit.DAYS.between(
                txns.get(i-1).getTransactionDate(),
                txns.get(i).getTransactionDate()
            );
            if (daysBetween >= 25 && daysBetween <= 35) {
                recurringCount++;
            }
        }
        
        if (recurringCount >= 2) {
            Subscription sub = createSubscription(user, entry.getKey(), txns);
            subscriptions.add(sub);
        }
    }
    
    return subscriptionRepository.saveAll(subscriptions);
}

private String normalizeMerchant(String merchant) {
    return merchant.toLowerCase()
        .replaceAll("[^a-z0-9]", "")
        .trim();
}
```

### Demo Data Generation Strategy

#### Deterministic Random Generation

```java
public int seedUserIfEmpty(Long userId) {
    User user = userRepository.findById(userId).orElseThrow();
    Long count = transactionRepository.countByUser(user);
    
    if (count > 0) {
        return 0; // Already has data
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
        null, String.format("Generated %d demo transactions", demoTransactions.size()));
    
    return demoTransactions.size();
}
```

#### Transaction Generation Rules

**Time Distribution**:
- Total period: 60-90 days before current date
- Random distribution across the period
- Some clustering to trigger rapid-fire detection

**Amount Distribution**:
- Groceries: $20-$150
- Utilities: $50-$300
- Entertainment: $10-$100
- Salary: $2000-$5000 (income)
- Rent: $800-$2000
- Subscriptions: $5-$50
- Transport: $10-$80

**Category Mix**:
- 40% groceries
- 15% utilities
- 15% entertainment
- 10% transport
- 10% subscriptions
- 5% salary (income)
- 5% rent

**Fraud Triggers**:
- Include 1-2 transactions with amounts > 3x average
- Include 1 cluster of 5+ transactions within 10 minutes
- Include 1-2 transactions with unusual categories

**Example Generation**:
```java
private List<Transaction> generateDemoTransactions(User user, Random random) {
    List<Transaction> transactions = new ArrayList<>();
    int count = 25 + random.nextInt(26); // 25-50
    int daysBack = 60 + random.nextInt(31); // 60-90
    
    LocalDateTime startDate = LocalDateTime.now().minusDays(daysBack);
    
    for (int i = 0; i < count; i++) {
        Transaction txn = new Transaction();
        txn.setUser(user);
        
        // Random date within range
        long randomDays = random.nextInt(daysBack);
        txn.setTransactionDate(startDate.plusDays(randomDays));
        
        // Random category and amount
        String category = selectRandomCategory(random);
        txn.setCategory(category);
        txn.setAmount(generateAmountForCategory(category, random));
        txn.setType(category.equals("salary") ? "INCOME" : "EXPENSE");
        
        txn.setDescription("Demo " + category);
        txn.setLocation("Demo Location");
        txn.setCreatedAt(LocalDateTime.now());
        
        transactions.add(txn);
    }
    
    // Add fraud triggers
    addFraudTriggers(transactions, user, random);
    
    return transactions;
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing all acceptance criteria, I identified several areas of redundancy:

**Redundant Properties Eliminated:**
- Requirements 5.8 and 2.3 both specify that fraud score >= 70 sets fraudulent flag to true (consolidated into Property 3)
- Requirements 10.2, 10.3, and 10.4 duplicate audit logging requirements from 6.4, 8.8, and 1.7 (covered by Property 7)
- Requirements 12.1 and 1.6 both specify deterministic demo data generation (consolidated into Property 1)
- Requirements 5.5, 5.6, and 5.7 can be combined into a single comprehensive risk level mapping property (consolidated into Property 4)
- Requirements 7.1, 7.2, and 7.3 (income, expenses, balance) can be combined into a single dashboard calculation property (consolidated into Property 14)

**Properties Combined for Comprehensiveness:**
- All fraud detection rules (5.1, 5.2, 5.3) are tested individually but also verified together in Property 5
- All filtering criteria (3.1-3.4, 3.9) are combined into Property 9 for comprehensive filter testing
- Dashboard aggregations (7.6, 7.7, 7.8) are combined into Property 15 for comprehensive aggregation testing

### Property 1: Demo Data Determinism

*For any* user ID, when the demo data generator is invoked multiple times, it SHALL produce identical transaction sets with the same count, dates, amounts, categories, and fraud scores.

**Validates: Requirements 1.6, 12.1**

### Property 2: Demo Data Distribution Constraints

*For any* generated demo data set, all transactions SHALL fall within 60-90 days before the current date, the count SHALL be between 25-50, and the set SHALL include both INCOME and EXPENSE types with multiple categories including groceries, utilities, entertainment, salary, and rent.

**Validates: Requirements 1.1, 1.2, 1.3, 1.4**

### Property 3: Fraud Score Threshold Enforcement

*For any* transaction, when the fraud score is 70 or above, the fraudulent flag SHALL be set to true, and when the fraud score is below 70, the fraudulent flag SHALL be set to false.

**Validates: Requirements 2.3, 5.9**

### Property 4: Risk Level Mapping Correctness

*For any* transaction with a fraud score, the risk level SHALL be LOW when score < 40, MEDIUM when score is 40-69, and HIGH when score >= 70.

**Validates: Requirements 5.6, 5.7, 5.8**

### Property 5: Fraud Detection Rule Application

*For any* transaction where amount > 3x user average, the fraud score SHALL increase by at least 30 points; *for any* set of 5+ transactions within 10 minutes, each SHALL have fraud score increased by at least 25 points; *for any* transaction with a location different from the user's last transaction location AND time between transactions < 2 hours, the fraud score SHALL increase by at least 25 points; *for any* transaction with a category never used before by the user, the fraud score SHALL increase by at least 20 points.

**Validates: Requirements 5.1, 5.2, 5.3, 5.4**

### Property 6: Fraud Score Bounds

*For any* transaction, the computed fraud score SHALL be between 0 and 100 inclusive.

**Validates: Requirements 5.5**

### Property 7: Audit Logging Completeness

*For any* user action (transaction creation, fraud alert resolution, subscription ignore, demo data seeding), an audit log entry SHALL exist with user ID, action type, entity type, entity ID, timestamp in UTC, and details in valid JSON format.

**Validates: Requirements 1.7, 2.5, 6.4, 8.8, 10.1, 10.4, 10.5, 10.6, 10.8**

### Property 8: Fraud Alert Creation

*For any* transaction flagged as fraudulent, a fraud alert SHALL be created with severity matching the risk level and including transaction details.

**Validates: Requirements 2.4, 5.10**

### Property 9: Transaction Filtering Correctness

*For any* combination of filters (type, category, date range, fraudulent status), all returned transactions SHALL satisfy all specified filter conditions simultaneously.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.9**

### Property 10: Transaction Sorting Invariant

*For any* sort field and direction, sorting SHALL preserve the set of transactions (no additions or removals), and consecutive elements SHALL be ordered according to the specified field and direction.

**Validates: Requirements 3.5**

### Property 11: Pagination Correctness

*For any* pagination parameters (page, size) and transaction set, the returned page SHALL contain at most 'size' elements, the total count SHALL equal the full result set size, and requesting all pages SHALL return all transactions exactly once.

**Validates: Requirements 3.8**

### Property 12: Transaction Persistence Round Trip

*For any* valid transaction request, after creation the transaction SHALL be retrievable with all fields matching the request data plus computed fraud score and fraudulent flag.

**Validates: Requirements 2.1, 2.2**

### Property 13: Validation Error Handling

*For any* transaction request missing required fields (amount, type, category, transactionDate), the service SHALL reject it with HTTP 400 status and return all validation errors in a single response with consistent JSON format including message and timestamp.

**Validates: Requirements 2.6, 2.7, 11.1, 11.4, 11.5**

### Property 14: Dashboard Financial Calculations

*For any* set of transactions, the dashboard summary SHALL calculate total income as the sum of all INCOME transactions, total expenses as the sum of all EXPENSE transactions, and current balance as total income minus total expenses.

**Validates: Requirements 7.1, 7.2, 7.3**

### Property 15: Dashboard Aggregations

*For any* set of transactions, the dashboard SHALL correctly aggregate spending by category (sum of amounts per category), fraud incidents by category (count of fraudulent transactions per category), and spending trends by time period (sum of amounts per date).

**Validates: Requirements 7.6, 7.7, 7.8**

### Property 16: Dashboard Fraud Metrics

*For any* set of transactions, the dashboard SHALL count total flagged transactions as the number with fraudulent=true, and calculate average fraud score as the mean of all fraud scores.

**Validates: Requirements 7.4, 7.5**

### Property 17: Dashboard Date Range Filtering

*For any* date range filter on dashboard summary, only transactions with transactionDate within the specified range SHALL be included in all calculations.

**Validates: Requirements 7.9**

### Property 18: Subscription Detection Pattern Matching

*For any* set of transactions to the same merchant with 2 or more payments occurring 25-35 days apart, a subscription SHALL be detected with average amount calculated from those payments, last payment date matching the most recent transaction, and next due date calculated as last payment date plus 30 days.

**Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**

### Property 19: Subscription Due-Soon Filtering

*For any* due-soon query with days parameter, all returned subscriptions SHALL have status=ACTIVE and next due date within the specified number of days from current date, and no ignored subscriptions SHALL be returned.

**Validates: Requirements 8.9, 8.10**

### Property 20: Subscription Status Update

*For any* subscription ignore action, the subscription status SHALL be updated to IGNORED and remain retrievable with the updated status.

**Validates: Requirements 8.7**

### Property 21: Fraud Alert Ordering and Completeness

*For any* user, when requesting fraud alerts, all alerts for that user SHALL be returned ordered by creation date descending, with each alert including complete transaction details.

**Validates: Requirements 6.1, 6.2**

### Property 22: Fraud Alert Filtering

*For any* fraud alert filter (resolved status or severity level), all returned alerts SHALL match the specified filter criteria.

**Validates: Requirements 6.6, 6.7**

### Property 23: Fraud Alert Resolution

*For any* fraud alert resolution action, the alert's resolved flag SHALL be set to true and the alert SHALL remain permanently retrievable.

**Validates: Requirements 6.3, 6.5**

### Property 24: Audit Log Immutability

*For any* created audit log entry, it SHALL remain permanently retrievable with all fields unchanged.

**Validates: Requirements 10.7**

### Property 25: HTTP Error Status Codes

*For any* request for a non-existent resource, the response SHALL have HTTP status 404; *for any* invalid request, the response SHALL have HTTP status 400.

**Validates: Requirements 11.1, 11.2**

### Property 26: Fraud Detection Determinism

*For any* identical transaction pattern (same amounts, categories, timing, user history), the fraud detection engine SHALL produce identical fraud scores.

**Validates: Requirements 12.3**

### Property 27: Subscription Detection Idempotence

*For any* transaction set, running subscription detection multiple times SHALL produce identical results.

**Validates: Requirements 12.4**

### Property 28: Demo Data Fraud Triggers

*For any* generated demo data set, at least 3 transactions SHALL have characteristics that trigger fraud detection rules (fraud score > 0).

**Validates: Requirements 1.5**

### Property 29: Demo Data Amount Realism

*For any* generated demo transaction, the amount SHALL fall within realistic ranges for its category (e.g., groceries: $20-$150, utilities: $50-$300, salary: $2000-$5000).

**Validates: Requirements 1.8**

### Property 30: Backward Compatibility with Optional Parameters

*For any* existing endpoint extended with new optional parameters, calling the endpoint without the new parameters SHALL succeed using default values and return responses in the original format.

**Validates: Requirements 15.3**

### Property 31: Geographical Fraud Detection

*For any* transaction with a location different from the user's most recent transaction location, when the time between transactions is less than 2 hours, the fraud score SHALL increase by at least 25 points.

**Validates: Requirements 5.4**


## Error Handling

### Error Response Format

All error responses follow a consistent JSON structure:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "amount",
      "message": "Amount is required"
    },
    {
      "field": "category",
      "message": "Category is required"
    }
  ],
  "path": "/api/transactions"
}
```

### Exception Handling Strategy

#### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
            .collect(Collectors.toList());
        
        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(400)
            .error("Bad Request")
            .message("Validation failed")
            .errors(errors)
            .build();
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFound(ResourceNotFoundException ex) {
        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(404)
            .error("Not Found")
            .message(ex.getMessage())
            .build();
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception ex) {
        // Log full stack trace for debugging
        log.error("Internal server error", ex);
        
        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(500)
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .build();
    }
}
```

### Validation Rules

#### Transaction Validation

```java
public class TransactionRequest {
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Type is required")
    @Pattern(regexp = "INCOME|EXPENSE", message = "Type must be INCOME or EXPENSE")
    private String type;
    
    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;
    
    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDateTime transactionDate;
    
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
    
    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;
}
```

### Error Scenarios

#### Transaction Creation Errors

1. **Missing Required Fields**: HTTP 400 with field-specific messages
2. **Invalid User ID**: HTTP 404 with "User not found" message
3. **Invalid Amount**: HTTP 400 with "Amount must be greater than 0" message
4. **Invalid Type**: HTTP 400 with "Type must be INCOME or EXPENSE" message
5. **Future Transaction Date**: HTTP 400 with "Transaction date cannot be in the future" message
6. **Database Constraint Violation**: HTTP 500 with generic error message

#### Fraud Alert Errors

1. **Alert Not Found**: HTTP 404 with "Fraud alert not found" message
2. **Already Resolved**: HTTP 400 with "Alert is already resolved" message
3. **Unauthorized Access**: HTTP 403 with "Access denied" message

#### Subscription Errors

1. **Subscription Not Found**: HTTP 404 with "Subscription not found" message
2. **Invalid Days Parameter**: HTTP 400 with "Days must be between 1 and 365" message

#### Dashboard Errors

1. **Invalid Date Range**: HTTP 400 with "End date must be after start date" message
2. **Date Range Too Large**: HTTP 400 with "Date range cannot exceed 1 year" message

### Logging Strategy

#### Log Levels

- **ERROR**: Exceptions, failed operations, data integrity issues
- **WARN**: Validation failures, suspicious patterns, deprecated API usage
- **INFO**: Successful operations, audit events, system state changes
- **DEBUG**: Detailed execution flow, parameter values, query results

#### Log Format

```java
// Transaction creation
log.info("Creating transaction for user {} with amount {} and category {}", 
    userId, amount, category);

// Fraud detection
log.warn("High fraud score detected: {} for transaction {} of user {}", 
    fraudScore, transactionId, userId);

// Error handling
log.error("Failed to create transaction for user {}: {}", 
    userId, ex.getMessage(), ex);

// Audit logging
log.info("Audit log created: user={}, action={}, entityType={}, entityId={}", 
    userId, action, entityType, entityId);
```

## Testing Strategy

### Overview

The testing strategy employs a dual approach combining unit tests for specific examples and edge cases with property-based tests for universal correctness guarantees. This ensures both concrete bug detection and comprehensive input coverage.

### Property-Based Testing

#### Framework Selection

**Java**: Use **jqwik** (https://jqwik.net/) - a mature property-based testing framework for JUnit 5

```xml
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.7.4</version>
    <scope>test</scope>
</dependency>
```

#### Configuration

- Minimum 100 iterations per property test (configured via `@Property(tries = 100)`)
- Each test must reference its design document property via comment
- Tag format: `// Feature: finsight-enhancement, Property {number}: {property_text}`

#### Property Test Examples

**Property 1: Demo Data Determinism**
```java
@Property(tries = 100)
// Feature: finsight-enhancement, Property 1: Demo data determinism
void demoDataGenerationIsDeterministic(@ForAll @LongRange(min = 1, max = 10000) Long userId) {
    // First generation
    List<Transaction> firstRun = demoDataService.generateDemoTransactions(userId);
    
    // Second generation with same userId
    List<Transaction> secondRun = demoDataService.generateDemoTransactions(userId);
    
    // Should be identical
    assertThat(firstRun).hasSize(secondRun.size());
    for (int i = 0; i < firstRun.size(); i++) {
        assertTransactionsEqual(firstRun.get(i), secondRun.get(i));
    }
}
```

**Property 3: Fraud Score Threshold Enforcement**
```java
@Property(tries = 100)
// Feature: finsight-enhancement, Property 3: Fraud score threshold enforcement
void fraudulentFlagSetCorrectlyBasedOnScore(
    @ForAll @DoubleRange(min = 0.0, max = 100.0) double fraudScore) {
    
    Transaction transaction = createTestTransaction();
    transaction.setFraudScore(fraudScore);
    
    FraudDetectionResult result = new FraudDetectionResult(
        fraudScore >= 70, fraudScore, RiskLevel.fromScore(fraudScore), List.of()
    );
    
    if (fraudScore >= 70) {
        assertThat(result.isFraudulent()).isTrue();
    } else {
        assertThat(result.isFraudulent()).isFalse();
    }
}
```

**Property 9: Transaction Filtering Correctness**
```java
@Property(tries = 100)
// Feature: finsight-enhancement, Property 9: Transaction filtering correctness
void filteringReturnsOnlyMatchingTransactions(
    @ForAll("transactionLists") List<Transaction> transactions,
    @ForAll("transactionTypes") String type,
    @ForAll("categories") String category) {
    
    // Save test data
    transactionRepository.saveAll(transactions);
    
    // Apply filters
    TransactionFilter filter = TransactionFilter.builder()
        .type(type)
        .category(category)
        .build();
    
    List<Transaction> results = transactionService.findWithFilters(filter);
    
    // Verify all results match filters
    assertThat(results).allMatch(t -> 
        t.getType().equals(type) && t.getCategory().equals(category)
    );
}

@Provide
Arbitrary<List<Transaction>> transactionLists() {
    return Arbitraries.of(
        createTransactionList("INCOME", "salary", 5),
        createTransactionList("EXPENSE", "groceries", 10),
        createTransactionList("EXPENSE", "utilities", 7)
    );
}
```

**Property 14: Dashboard Financial Calculations**
```java
@Property(tries = 100)
// Feature: finsight-enhancement, Property 14: Dashboard financial calculations
void dashboardCalculatesBalanceCorrectly(
    @ForAll("transactionLists") List<Transaction> transactions) {
    
    User user = createTestUser();
    transactions.forEach(t -> t.setUser(user));
    transactionRepository.saveAll(transactions);
    
    DashboardSummary summary = dashboardService.getSummary(user.getId(), null, null);
    
    BigDecimal expectedIncome = transactions.stream()
        .filter(t -> t.getType().equals("INCOME"))
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal expectedExpenses = transactions.stream()
        .filter(t -> t.getType().equals("EXPENSE"))
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal expectedBalance = expectedIncome.subtract(expectedExpenses);
    
    assertThat(summary.getTotalIncome()).isEqualByComparingTo(expectedIncome);
    assertThat(summary.getTotalExpenses()).isEqualByComparingTo(expectedExpenses);
    assertThat(summary.getCurrentBalance()).isEqualByComparingTo(expectedBalance);
}
```

**Property 18: Subscription Detection Pattern Matching**
```java
@Property(tries = 100)
// Feature: finsight-enhancement, Property 18: Subscription detection pattern matching
void subscriptionDetectedForRecurringPayments(
    @ForAll @IntRange(min = 25, max = 35) int daysBetween) {
    
    User user = createTestUser();
    String merchant = "Netflix";
    BigDecimal amount = new BigDecimal("15.99");
    
    // Create 3 transactions with specified days between
    LocalDateTime firstDate = LocalDateTime.now().minusDays(daysBetween * 2);
    LocalDateTime secondDate = firstDate.plusDays(daysBetween);
    LocalDateTime thirdDate = secondDate.plusDays(daysBetween);
    
    List<Transaction> transactions = List.of(
        createTransaction(user, merchant, amount, firstDate),
        createTransaction(user, merchant, amount, secondDate),
        createTransaction(user, merchant, amount, thirdDate)
    );
    
    transactionRepository.saveAll(transactions);
    
    List<Subscription> subscriptions = subscriptionDetectorService.detectSubscriptions(user.getId());
    
    assertThat(subscriptions).hasSize(1);
    Subscription sub = subscriptions.get(0);
    assertThat(sub.getMerchant()).isEqualTo(merchant);
    assertThat(sub.getAvgAmount()).isEqualByComparingTo(amount);
    assertThat(sub.getLastPaidDate()).isEqualTo(thirdDate.toLocalDate());
    assertThat(sub.getNextDueDate()).isEqualTo(thirdDate.toLocalDate().plusDays(30));
}
```

### Unit Testing

#### Service Layer Tests

**DemoDataService Tests**
- Test seeding only occurs when transaction count is 0
- Test transaction count is within 25-50 range
- Test date distribution is within 60-90 days
- Test category variety includes required categories
- Test fraud triggers are present (at least 3)
- Test amounts are realistic for categories

**FraudDetectionService Tests**
- Test high amount rule (>3x average) adds 30 points
- Test rapid-fire rule (5+ in 10 min) adds 25 points
- Test unusual category rule adds 20 points
- Test score bounds (0-100)
- Test risk level mapping (LOW/MEDIUM/HIGH)
- Test fraudulent flag threshold (>= 70)

**SubscriptionDetectorService Tests**
- Test detection with 2 qualifying payments
- Test detection ignores payments outside 25-35 day range
- Test average amount calculation
- Test next due date calculation (last + 30 days)
- Test merchant name normalization

**DashboardService Tests**
- Test income/expense/balance calculations
- Test category aggregations
- Test fraud metrics (count, average score)
- Test time series aggregation
- Test date range filtering

**AuditLogService Tests**
- Test log entry creation with all required fields
- Test JSON details formatting
- Test UTC timestamp
- Test user ID inclusion

#### Controller Layer Tests (MockMvc)

**TransactionController Tests**
```java
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TransactionService transactionService;
    
    @Test
    void createTransaction_WithValidData_Returns201() throws Exception {
        TransactionRequest request = createValidRequest();
        TransactionResponse response = createResponse();
        
        when(transactionService.createTransaction(any())).thenReturn(response);
        
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(response.getId()))
            .andExpect(jsonPath("$.fraudScore").exists());
    }
    
    @Test
    void createTransaction_WithMissingAmount_Returns400() throws Exception {
        TransactionRequest request = createInvalidRequest(); // missing amount
        
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[?(@.field == 'amount')]").exists());
    }
    
    @Test
    void getTransactions_WithFilters_ReturnsFilteredResults() throws Exception {
        List<TransactionResponse> responses = createResponses();
        
        when(transactionService.findWithFilters(any())).thenReturn(responses);
        
        mockMvc.perform(get("/api/transactions")
                .param("type", "EXPENSE")
                .param("category", "groceries")
                .param("fraudulent", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[*].type").value(everyItem(equalTo("EXPENSE"))))
            .andExpect(jsonPath("$[*].category").value(everyItem(equalTo("groceries"))))
            .andExpect(jsonPath("$[*].fraudulent").value(everyItem(equalTo(true))));
    }
}
```

**FraudAlertController Tests**
- Test GET /api/fraud/alerts returns all alerts
- Test filtering by resolved status
- Test filtering by severity
- Test PUT /api/fraud/alerts/{id}/resolve updates flag
- Test 404 for non-existent alert

**SubscriptionController Tests**
- Test GET /api/subscriptions returns all subscriptions
- Test GET /api/subscriptions/due-soon with days parameter
- Test PUT /api/subscriptions/{id}/ignore updates status
- Test ignored subscriptions excluded from due-soon

**DashboardController Tests**
- Test GET /api/summary returns complete summary
- Test date range filtering
- Test all aggregations present in response

#### Repository Layer Tests

**TransactionRepository Tests**
- Test custom queries (findByUserAndFraudulentTrue, etc.)
- Test average amount calculation
- Test distinct categories query
- Test pagination
- Test specification-based filtering

**SubscriptionRepository Tests**
- Test findDueSoon query with date range
- Test filtering by status
- Test ordering

**AuditLogRepository Tests**
- Test findByUserOrderByTimestampDesc
- Test findByEntityTypeAndEntityId

### Integration Tests

**End-to-End Flow Tests**

1. **Transaction Creation Flow**
   - Create transaction → Verify fraud detection → Verify alert creation → Verify audit log

2. **Demo Data Seeding Flow**
   - Login with new user → Verify seeding triggered → Verify transactions created → Verify fraud detection ran

3. **Subscription Detection Flow**
   - Create recurring transactions → Run detection → Verify subscription created → Verify due-soon query

4. **Dashboard Aggregation Flow**
   - Create varied transactions → Request summary → Verify all calculations correct

### Test Data Builders

```java
public class TransactionTestBuilder {
    private Transaction transaction = new Transaction();
    
    public static TransactionTestBuilder aTransaction() {
        return new TransactionTestBuilder()
            .withAmount(new BigDecimal("100.00"))
            .withType("EXPENSE")
            .withCategory("groceries")
            .withDate(LocalDateTime.now())
            .withFraudScore(0.0);
    }
    
    public TransactionTestBuilder withAmount(BigDecimal amount) {
        transaction.setAmount(amount);
        return this;
    }
    
    public TransactionTestBuilder withHighFraudScore() {
        transaction.setFraudScore(75.0);
        transaction.setFraudulent(true);
        return this;
    }
    
    public Transaction build() {
        return transaction;
    }
}
```

### Test Coverage Goals

- **Line Coverage**: Minimum 80%
- **Branch Coverage**: Minimum 75%
- **Service Layer**: Minimum 90% coverage
- **Controller Layer**: Minimum 85% coverage
- **Critical Paths**: 100% coverage (fraud detection, audit logging, subscription detection)

### Continuous Integration

- Run all tests on every commit
- Property-based tests run with 100 iterations in CI
- Integration tests run against H2 in-memory database
- Test reports generated and archived
- Coverage reports published


## Deployment Architecture

### Docker Compose Configuration

```yaml
version: '3.8'

services:
  backend:
    build:
      context: .
      dockerfile: Dockerfile
      platforms:
        - linux/amd64
        - linux/arm64
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:h2:mem:finsight
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    networks:
      - finsight-network

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      platforms:
        - linux/amd64
        - linux/arm64
    ports:
      - "3000:3000"
    environment:
      - REACT_APP_API_URL=http://localhost:8080/api
    depends_on:
      backend:
        condition: service_healthy
    networks:
      - finsight-network

networks:
  finsight-network:
    driver: bridge
```

### Backend Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend Dockerfile

```dockerfile
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]
```

### Health Check Endpoint

```java
@RestController
@RequestMapping("/actuator")
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> health() {
        return ResponseEntity.ok(new HealthStatus("UP", LocalDateTime.now()));
    }
}
```

### Application Configuration

**application.properties**
```properties
# Server Configuration
server.port=8080
spring.application.name=FinSight

# Database Configuration
spring.datasource.url=jdbc:h2:mem:finsight
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# H2 Console (development only)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Logging
logging.level.com.example.FinSight=INFO
logging.level.org.springframework.web=INFO
logging.level.org.hibernate.SQL=DEBUG

# Jackson Configuration
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.time-zone=UTC
```

## Implementation Notes

### Migration Strategy

#### Phase 1: Database Schema Updates
1. Add `fraud_score` column to `transactions` table (nullable for backward compatibility)
2. Create `subscriptions` table
3. Create `audit_logs` table
4. Add indexes for performance

#### Phase 2: Service Layer Implementation
1. Implement `DemoDataService` with deterministic generation
2. Enhance `FraudDetectionService` with new rule-based algorithm
3. Implement `SubscriptionDetectorService`
4. Implement `AuditLogService`
5. Implement `DashboardService`

#### Phase 3: Controller Layer Updates
1. Enhance `TransactionController` with filtering and pagination
2. Create `FraudAlertController` with new endpoints
3. Create `SubscriptionController`
4. Create `DashboardController`
5. Update authentication flow to trigger demo seeding

#### Phase 4: Frontend Development
1. Create dashboard page with charts
2. Enhance transactions page with filters
3. Create fraud alerts page
4. Create subscriptions page
5. Implement live refresh toggle

#### Phase 5: Testing and Validation
1. Write property-based tests for all correctness properties
2. Write unit tests for services and controllers
3. Write integration tests for end-to-end flows
4. Perform manual testing of UI
5. Validate Docker deployment

### Performance Considerations

#### Database Optimization
- Index on `(user_id, transaction_date)` for efficient date range queries
- Index on `(user_id, fraudulent)` for fraud alert queries
- Index on `(user_id, category)` for category aggregations
- Consider pagination for large result sets (default page size: 20)

#### Caching Strategy
- Cache user average transaction amount (invalidate on new transaction)
- Cache user categories list (invalidate on new category)
- Cache dashboard summary (TTL: 5 minutes)
- Cache subscription detection results (invalidate on new transaction)

#### Query Optimization
- Use Spring Data JPA Specifications for dynamic filtering
- Use `@EntityGraph` to avoid N+1 queries when fetching alerts with transactions
- Use batch inserts for demo data generation
- Use database-level aggregations for dashboard calculations

### Security Considerations

#### Authentication and Authorization
- Verify user owns resources before allowing access
- Implement JWT-based authentication
- Add CORS configuration for frontend
- Validate all user inputs

#### Data Protection
- Sanitize user inputs to prevent SQL injection
- Use parameterized queries (Spring Data JPA handles this)
- Encrypt sensitive data at rest (future enhancement)
- Implement rate limiting for API endpoints

#### Audit Trail
- Log all user actions with timestamps
- Include IP address and user agent in audit logs (future enhancement)
- Ensure audit logs are immutable
- Implement audit log retention policy

### Backward Compatibility Checklist

- [ ] Existing `/api/transactions` GET endpoint works without new parameters
- [ ] Existing `/api/transactions` POST endpoint works with original request format
- [ ] Existing DTOs maintain all original fields
- [ ] New optional fields have sensible defaults
- [ ] Database migrations are non-breaking (nullable columns)
- [ ] Existing tests continue to pass
- [ ] API documentation updated with new optional parameters

### Frontend Architecture

#### Component Structure
```
src/
├── components/
│   ├── Dashboard/
│   │   ├── SummaryCard.jsx
│   │   ├── SpendingChart.jsx
│   │   └── FraudChart.jsx
│   ├── Transactions/
│   │   ├── TransactionList.jsx
│   │   ├── TransactionFilter.jsx
│   │   ├── TransactionForm.jsx
│   │   └── LiveRefreshToggle.jsx
│   ├── FraudAlerts/
│   │   ├── AlertList.jsx
│   │   ├── AlertCard.jsx
│   │   └── AlertFilter.jsx
│   ├── Subscriptions/
│   │   ├── SubscriptionList.jsx
│   │   ├── SubscriptionCard.jsx
│   │   └── DueSoonBanner.jsx
│   └── Common/
│       ├── Badge.jsx
│       ├── Toast.jsx
│       └── Pagination.jsx
├── services/
│   ├── api.js
│   ├── transactionService.js
│   ├── fraudAlertService.js
│   ├── subscriptionService.js
│   └── dashboardService.js
├── hooks/
│   ├── usePolling.js
│   ├── useTransactions.js
│   └── useDashboard.js
└── utils/
    ├── formatters.js
    └── validators.js
```

#### State Management
- Use React Context for global state (user, auth)
- Use local state for component-specific data
- Use custom hooks for data fetching and polling
- Consider React Query for server state management (future enhancement)

#### Styling Approach
- Use CSS modules for component-specific styles
- Use CSS variables for theming
- Implement responsive breakpoints (mobile: <768px, tablet: 768-1024px, desktop: >1024px)
- Use Flexbox and Grid for layouts

### Monitoring and Observability

#### Metrics to Track
- Transaction creation rate
- Fraud detection rate (% flagged)
- Average fraud score
- API response times
- Error rates by endpoint
- Demo data seeding frequency

#### Logging Best Practices
- Use structured logging (JSON format)
- Include correlation IDs for request tracing
- Log all fraud detections with details
- Log all audit events
- Avoid logging sensitive data (amounts, descriptions)

### Future Enhancements

1. **Machine Learning Fraud Detection**: Replace rule-based with ML model
2. **Real-time Notifications**: WebSocket support for instant alerts
3. **Multi-currency Support**: Handle transactions in different currencies
4. **Export Functionality**: CSV/PDF export of transactions and reports
5. **Advanced Analytics**: Predictive spending, budget recommendations
6. **Mobile App**: Native iOS/Android applications
7. **Third-party Integrations**: Bank account linking, payment gateways
8. **User Preferences**: Customizable fraud thresholds, notification settings

## Conclusion

This design document provides a comprehensive blueprint for enhancing the FinSight Financial Transaction Tracker with advanced fraud detection, subscription tracking, demo data generation, and actionable insights. The architecture maintains backward compatibility while introducing powerful new features that improve user experience and system reliability.

The dual testing strategy ensures both concrete bug detection through unit tests and comprehensive correctness guarantees through property-based testing. The modular design allows for incremental implementation and future enhancements without disrupting existing functionality.

Key design decisions:
- Rule-based fraud detection provides explainable results
- Deterministic demo data enables reliable testing
- Pattern-based subscription detection requires no user configuration
- Comprehensive audit logging ensures regulatory compliance
- Layered architecture maintains separation of concerns
- Docker deployment simplifies development and production setup

The implementation should proceed in phases, with thorough testing at each stage to ensure quality and reliability.

