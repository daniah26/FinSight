# Implementation Plan: FinSight Enhancement

## Overview

This implementation plan breaks down the FinSight enhancement into discrete coding tasks that build incrementally on the existing Spring Boot application. The plan follows a phased approach: database schema updates, service layer implementation, controller layer updates, frontend development, and testing. Each task includes specific requirements references and builds on previous work to ensure no orphaned code.

## Tasks

- [x] 1. Set up database schema and entity enhancements
  - Add fraud_score column to Transaction entity (nullable for backward compatibility)
  - Create Subscription entity with all required fields
  - Create AuditLog entity with all required fields
  - Create RiskLevel and SubscriptionStatus enums
  - Add database indexes for performance optimization
  - _Requirements: 2.2, 5.4, 8.1-8.5, 10.1-10.8_

- [ ] 2. Implement core service layer components
  - [x] 2.1 Create AuditLogService with logging functionality
    - Implement logAction method with all required parameters
    - Ensure UTC timestamps and JSON details formatting
    - Create AuditLogRepository interface
    - _Requirements: 1.7, 2.5, 6.4, 8.8, 10.1-10.8_

  - [ ]* 2.2 Write property test for AuditLogService
    - **Property 7: Audit Logging Completeness**
    - **Validates: Requirements 1.7, 2.5, 6.4, 8.8, 10.1-10.8**

  - [ ]* 2.3 Write property test for audit log immutability
    - **Property 24: Audit Log Immutability**
    - **Validates: Requirements 10.7**

  - [x] 2.4 Implement FraudDetectionService with rule-based algorithm
    - Implement analyzeTransaction method with four fraud rules
    - Rule 1: High amount anomaly (>3x average) adds 30 points
    - Rule 2: Rapid-fire activity (5+ in 10 min) adds 25 points
    - Rule 3: Geographical anomaly (different location < 2 hours) adds 25 points
    - Rule 4: Unusual category adds 20 points
    - Implement calculateRiskLevel method for score-to-level mapping
    - Create FraudDetectionResult class
    - _Requirements: 5.1-5.10_

  - [x] 2.5 Write property tests for fraud detection rules
    - **Property 5: Fraud Detection Rule Application**
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4**

  - [x] 2.6 Write property test for fraud score bounds
    - **Property 6: Fraud Score Bounds**
    - **Validates: Requirements 5.5**

  - [x] 2.7 Write property test for fraud score threshold enforcement
    - **Property 3: Fraud Score Threshold Enforcement**
    - **Validates: Requirements 2.3, 5.9**

  - [x] 2.8 Write property test for risk level mapping
    - **Property 4: Risk Level Mapping Correctness**
    - **Validates: Requirements 5.6, 5.7, 5.8**

  - [x] 2.9 Write property test for fraud detection determinism
    - **Property 26: Fraud Detection Determinism**
    - **Validates: Requirements 12.3**

  - [x] 2.10 Write unit tests for FraudDetectionService
    - Test each fraud rule individually with specific examples
    - Test geographical anomaly with different locations and time windows
    - Test edge cases (zero transactions, single transaction, etc.)
    - _Requirements: 5.1-5.10_

  - [x] 2.11 Write property test for geographical fraud detection
    - **Property 31: Geographical Fraud Detection**
    - **Validates: Requirements 5.4**

- [x] 3. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Implement DemoDataService for first-time user seeding
  - [x] 4.1 Create DemoDataService with deterministic generation
    - Implement seedUserIfEmpty method with transaction count check
    - Implement generateDemoTransactions with seeded Random(userId.hashCode())
    - Generate 25-50 transactions distributed across 60-90 days
    - Include varied categories (groceries, utilities, entertainment, salary, rent)
    - Include both INCOME and EXPENSE types
    - Ensure realistic amounts per category
    - Include fraud triggers (high amounts, rapid-fire, unusual categories)
    - Integrate with FraudDetectionService for each generated transaction
    - Integrate with AuditLogService to log seeding action
    - _Requirements: 1.1-1.8, 12.1, 12.2_

  - [ ]* 4.2 Write property test for demo data determinism
    - **Property 1: Demo Data Determinism**
    - **Validates: Requirements 1.6, 12.1**

  - [ ]* 4.3 Write property test for demo data distribution constraints
    - **Property 2: Demo Data Distribution Constraints**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4**

  - [ ]* 4.4 Write property test for demo data fraud triggers
    - **Property 28: Demo Data Fraud Triggers**
    - **Validates: Requirements 1.5**

  - [ ]* 4.5 Write property test for demo data amount realism
    - **Property 29: Demo Data Amount Realism**
    - **Validates: Requirements 1.8**

  - [ ]* 4.6 Write unit tests for DemoDataService
    - Test seeding only occurs when count is 0
    - Test transaction count range (25-50)
    - Test date distribution (60-90 days)
    - Test category variety
    - _Requirements: 1.1-1.8_

- [ ] 5. Implement SubscriptionDetectorService
  - [x] 5.1 Create SubscriptionDetectorService with pattern detection
    - Implement detectSubscriptions method
    - Group transactions by normalized merchant name
    - Identify recurring patterns (25-35 days apart, minimum 2 occurrences)
    - Calculate average amount, last payment date, next due date
    - Create Subscription entities with ACTIVE status
    - Implement findDueSoon method with date range filtering
    - Create SubscriptionRepository interface
    - _Requirements: 8.1-8.10_

  - [x] 5.2 Write property test for subscription detection pattern matching
    - **Property 18: Subscription Detection Pattern Matching**
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**

  - [x] 5.3 Write property test for subscription due-soon filtering
    - **Property 19: Subscription Due-Soon Filtering**
    - **Validates: Requirements 8.9, 8.10**

  - [ ]* 5.4 Write property test for subscription detection idempotence
    - **Property 27: Subscription Detection Idempotence**
    - **Validates: Requirements 12.4**

  - [x] 5.5 Write unit tests for SubscriptionDetectorService
    - Test detection with 2 qualifying payments
    - Test detection ignores payments outside 25-35 day range
    - Test merchant name normalization
    - Test average amount calculation
    - _Requirements: 8.1-8.10_

- [x] 6. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Implement DashboardService for aggregated insights
  - [x] 7.1 Create DashboardService with summary calculations
    - Implement getSummary method with optional date range filtering
    - Calculate total income, total expenses, current balance
    - Count total flagged transactions
    - Calculate average fraud score
    - Aggregate spending by category
    - Aggregate fraud incidents by category
    - Aggregate spending trends by time period
    - Create DashboardSummary and TimeSeriesPoint DTOs
    - _Requirements: 7.1-7.9_

  - [x] 7.2 Write property test for dashboard financial calculations
    - **Property 14: Dashboard Financial Calculations**
    - **Validates: Requirements 7.1, 7.2, 7.3**

  - [ ]* 7.3 Write property test for dashboard aggregations
    - **Property 15: Dashboard Aggregations**
    - **Validates: Requirements 7.6, 7.7, 7.8**

  - [x] 7.4 Write property test for dashboard fraud metrics
    - **Property 16: Dashboard Fraud Metrics**
    - **Validates: Requirements 7.4, 7.5**

  - [x] 7.5 Write property test for dashboard date range filtering
    - **Property 17: Dashboard Date Range Filtering**
    - **Validates: Requirements 7.9**

  - [ ] 7.6 Write unit tests for DashboardService
    - Test income/expense/balance calculations with specific examples
    - Test category aggregations
    - Test fraud metrics
    - Test date range filtering edge cases
    - _Requirements: 7.1-7.9_

- [ ] 8. Enhance TransactionService with filtering and fraud integration
  - [x] 8.1 Update TransactionService for manual transaction creation
    - Integrate FraudDetectionService.analyzeTransaction in createTransaction
    - Set fraudulent flag and fraud score based on detection result
    - Create FraudAlert if fraudulent flag is true
    - Integrate AuditLogService to log transaction creation
    - Add validation for required fields
    - Create TransactionRequest and TransactionResponse DTOs
    - _Requirements: 2.1-2.7_

  - [ ]* 8.2 Write property test for transaction persistence round trip
    - **Property 12: Transaction Persistence Round Trip**
    - **Validates: Requirements 2.1, 2.2**

  - [-] 8.3 Write property test for fraud alert creation
    - **Property 8: Fraud Alert Creation**
    - **Validates: Requirements 2.4, 5.10**

  - [-] 8.4 Write property test for validation error handling
    - **Property 13: Validation Error Handling**
    - **Validates: Requirements 2.6, 2.7, 11.1, 11.4, 11.5**

  - [x] 8.5 Implement transaction filtering with Spring Data Specifications
    - Create TransactionSpecification for dynamic filtering
    - Support filters: type, category, date range, fraudulent status
    - Implement findWithFilters method in TransactionService
    - Enhance TransactionRepository with Specification support
    - _Requirements: 3.1-3.4, 3.9_

  - [-] 8.6 Write property test for transaction filtering correctness
    - **Property 9: Transaction Filtering Correctness**
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.9**

  - [x] 8.7 Implement transaction sorting and pagination
    - Add Pageable support to findWithFilters method
    - Support sorting by transactionDate, amount, fraudScore, category
    - Support ASC and DESC directions
    - _Requirements: 3.5, 3.6, 3.7, 3.8_

  - [-] 8.8 Write property test for transaction sorting invariant
    - **Property 10: Transaction Sorting Invariant**
    - **Validates: Requirements 3.5**

  - [ ]* 8.9 Write property test for pagination correctness
    - **Property 11: Pagination Correctness**
    - **Validates: Requirements 3.8**

  - [-] 8.10 Write unit tests for TransactionService
    - Test manual transaction creation with fraud detection
    - Test filtering with various combinations
    - Test sorting by different fields
    - Test pagination edge cases
    - _Requirements: 2.1-2.7, 3.1-3.9_

- [ ] 9. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 10. Implement FraudAlertService for alert management
  - [x] 10.1 Create FraudAlertService with alert operations
    - Implement findByUser method with ordering by creation date descending
    - Implement resolveAlert method to set resolved flag
    - Integrate AuditLogService to log resolution action
    - Implement filtering by resolved status and severity
    - Create FraudAlertDto with transaction details
    - Create FraudAlertRepository interface
    - _Requirements: 6.1-6.7_

  - [ ]* 10.4 Write property test for fraud alert resolution
    - **Property 23: Fraud Alert Resolution**
    - **Validates: Requirements 6.3, 6.5**

  - [ ] 10.5 Write unit tests for FraudAlertService
    - Test alert retrieval with ordering
    - Test filtering by resolved status
    - Test filtering by severity
    - Test alert resolution
    - _Requirements: 6.1-6.7_

- [ ] 11. Create and enhance REST controllers
  - [ ] 11.1 Enhance TransactionController with new endpoints
    - Update POST /api/transactions to return fraud score in response
    - Update GET /api/transactions to support filtering query parameters
    - Add query parameters: type, category, startDate, endDate, fraudulent
    - Add query parameters: sortBy, sortDir, page, size
    - Ensure backward compatibility with existing endpoints
    - Add @Valid annotations for request validation
    - _Requirements: 2.1-2.7, 3.1-3.9, 15.1-15.4_

  - [ ]* 11.2 Write property test for backward compatibility
    - **Property 30: Backward Compatibility with Optional Parameters**
    - **Validates: Requirements 15.3**

  - [ ] 11.3 Create FraudAlertController with alert endpoints
    - Implement GET /api/fraud/alerts with filtering
    - Add query parameters: resolved, severity
    - Implement PUT /api/fraud/alerts/{id}/resolve
    - Return FraudAlertDto with transaction details
    - _Requirements: 6.1-6.7_

  - [ ] 11.4 Create SubscriptionController with subscription endpoints
    - Implement GET /api/subscriptions to return all subscriptions
    - Implement GET /api/subscriptions/due-soon with days parameter
    - Implement PUT /api/subscriptions/{id}/ignore to update status
    - Return SubscriptionDto in responses
    - _Requirements: 8.6-8.10_

  - [ ]* 11.5 Write property test for subscription status update
    - **Property 20: Subscription Status Update**
    - **Validates: Requirements 8.7**

  - [ ] 11.6 Create DashboardController with summary endpoint
    - Implement GET /api/summary with optional date range parameters
    - Add query parameters: startDate, endDate
    - Return DashboardSummary with all aggregations
    - _Requirements: 7.1-7.9_

  - [ ] 11.7 Enhance AuthController to trigger demo data seeding
    - Update POST /api/auth/login to check transaction count
    - Call DemoDataService.seedUserIfEmpty after successful login
    - Ensure seeding only occurs when count is 0
    - _Requirements: 1.1-1.8_

  - [ ]* 11.8 Write property test for HTTP error status codes
    - **Property 25: HTTP Error Status Codes**
    - **Validates: Requirements 11.1, 11.2**

  - [ ] 11.9 Write integration tests for controller endpoints
    - Test transaction creation end-to-end flow
    - Test filtering and pagination
    - Test fraud alert resolution
    - Test subscription detection and due-soon queries
    - Test dashboard summary calculations
    - _Requirements: 2.1-2.7, 3.1-3.9, 6.1-6.7, 7.1-7.9, 8.1-8.10_

- [ ] 12. Implement global exception handler
  - [ ] 12.1 Create GlobalExceptionHandler for consistent error responses
    - Handle MethodArgumentNotValidException for validation errors
    - Handle ResourceNotFoundException for 404 errors
    - Handle generic Exception for 500 errors
    - Return ErrorResponse with consistent JSON format
    - Include timestamp, status, error, message, and errors fields
    - Return all validation errors in a single response
    - _Requirements: 11.1-11.6_

  - [ ]* 12.2 Write unit tests for GlobalExceptionHandler
    - Test validation error response format
    - Test 404 error response format
    - Test 500 error response format
    - Test multiple validation errors in single response
    - _Requirements: 11.1-11.6_

- [ ] 13. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 14. Create React frontend components
  - [ ] 14.1 Set up frontend project structure and dependencies
    - Create React app with required dependencies
    - Set up API service layer with axios
    - Create utility functions for formatting and validation
    - Set up CSS modules for styling
    - _Requirements: 14.1-14.10_

  - [ ] 14.2 Implement authentication pages
    - Create Login component with form validation
    - Create Registration component
    - Implement authentication context for global state
    - Store JWT token in localStorage
    - _Requirements: 14.1_

  - [ ] 14.3 Create Dashboard page with summary cards and charts
    - Create SummaryCard component for income/expenses/balance
    - Create SpendingChart component for category breakdown
    - Create FraudChart component for fraud metrics
    - Implement date range filter controls
    - Fetch data from GET /api/summary endpoint
    - _Requirements: 7.1-7.9, 14.3_

  - [ ] 14.4 Create Transactions page with filtering and live refresh
    - Create TransactionList component with table display
    - Create TransactionFilter component with all filter controls
    - Create TransactionForm component for manual entry
    - Create LiveRefreshToggle component with polling logic
    - Implement usePolling custom hook (5-10 second interval)
    - Fetch data from GET /api/transactions endpoint
    - Display fraud score and risk level badges
    - _Requirements: 2.1-2.7, 3.1-3.9, 4.1-4.5, 14.2, 14.7_

  - [ ] 14.5 Create Fraud Alerts page with resolution controls
    - Create AlertList component with alert cards
    - Create AlertCard component with transaction details
    - Create AlertFilter component for resolved/severity filters
    - Implement resolve button with PUT /api/fraud/alerts/{id}/resolve
    - Display severity badges with color coding
    - Display resolved/unresolved status badges
    - _Requirements: 6.1-6.7, 14.4, 14.7, 14.8_

  - [ ] 14.6 Create Subscriptions page with due-soon notifications
    - Create SubscriptionList component with subscription cards
    - Create SubscriptionCard component with merchant and amount
    - Create DueSoonBanner component for upcoming payments
    - Implement ignore button with PUT /api/subscriptions/{id}/ignore
    - Fetch data from GET /api/subscriptions and GET /api/subscriptions/due-soon
    - Highlight subscriptions due within 7 days
    - _Requirements: 8.6-8.10, 9.1-9.5, 14.5, 14.10_

  - [ ] 14.7 Implement common UI components
    - Create Badge component for risk levels and statuses
    - Create Toast component for success/error notifications
    - Create Pagination component for transaction lists
    - Implement responsive CSS with mobile/tablet/desktop breakpoints
    - _Requirements: 14.6, 14.7, 14.8, 14.9_

- [ ] 15. Configure Docker deployment
  - [ ] 15.1 Create Docker configuration files
    - Create backend Dockerfile with multi-stage build
    - Create frontend Dockerfile with nginx
    - Create docker-compose.yml with backend and frontend services
    - Configure health checks for both services
    - Ensure compatibility with Apple Silicon (linux/amd64, linux/arm64)
    - Expose backend on port 8080 and frontend on port 3000
    - _Requirements: 13.1-13.6_

  - [ ] 15.2 Create health check endpoint
    - Implement GET /actuator/health endpoint in HealthController
    - Return status and timestamp in response
    - _Requirements: 13.6_

  - [ ]* 15.3 Test Docker deployment
    - Run docker compose up --build
    - Verify backend starts and health check passes
    - Verify frontend starts and connects to backend
    - Test end-to-end user flow in Docker environment
    - _Requirements: 13.1-13.6_

- [ ] 16. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at reasonable breaks
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- Integration tests validate end-to-end flows
- All tasks build incrementally with no orphaned code
- Backward compatibility is maintained throughout implementation
