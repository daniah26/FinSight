# Requirements Document

## Introduction

This document specifies requirements for enhancing the FinSight Financial Transaction Tracker to support near real-time transaction tracking, rule-based fraud detection with risk scoring, actionable insights through dashboards, basic regulatory compliance, advanced filtering capabilities, subscription tracking with notifications, and automated demo data generation for first-time users.

The enhancement builds upon the existing Spring Boot application with User, Transaction, and FraudAlert entities, extending functionality without breaking existing endpoints.

## Glossary

- **FinSight_System**: The complete financial transaction tracking and fraud detection application
- **Transaction_Service**: Backend service managing transaction CRUD operations and filtering
- **Fraud_Detection_Engine**: Service that analyzes transactions using rule-based algorithms to compute fraud scores
- **Demo_Data_Generator**: Service that creates realistic sample transactions for first-time users
- **Subscription_Detector**: Service that identifies recurring monthly payments from transaction patterns
- **Audit_Logger**: Service that records all system actions for compliance purposes
- **Dashboard_Service**: Service that aggregates transaction and fraud data for insights
- **Frontend_Client**: React-based user interface application
- **Risk_Level**: Enumeration of fraud risk categories (LOW, MEDIUM, HIGH)
- **Fraud_Score**: Numerical value from 0-100 indicating likelihood of fraudulent activity
- **Fraud_Alert**: Immutable record of detected suspicious activity requiring user review
- **Subscription**: Detected recurring monthly payment to the same merchant
- **Audit_Log**: Immutable record of user actions for compliance tracking
- **Demo_Transaction**: System-generated sample transaction created during first login
- **Manual_Transaction**: User-created transaction entry
- **Transaction_Filter**: Query parameters for filtering and sorting transaction lists
- **Polling_Interval**: Time period between frontend refresh requests (5-10 seconds)

## Requirements

### Requirement 1: Demo Data Generation for First-Time Users

**User Story:** As a new user, I want to see realistic demo transactions when I first log in, so that I can explore the system's features without manually entering data.

#### Acceptance Criteria

1. WHEN a User logs in AND THE User has zero transactions, THE Demo_Data_Generator SHALL create between 25 and 50 Demo_Transactions
2. THE Demo_Data_Generator SHALL distribute Demo_Transactions across the previous 60 to 90 days from the current date
3. THE Demo_Data_Generator SHALL create Demo_Transactions with varied categories including groceries, utilities, entertainment, salary, and rent
4. THE Demo_Data_Generator SHALL include both income and expense transaction types in the generated data
5. THE Demo_Data_Generator SHALL create at least 3 Demo_Transactions with characteristics that trigger fraud detection rules
6. WHEN generating Demo_Transactions for a specific User, THE Demo_Data_Generator SHALL use a deterministic seed based on the User ID to ensure reproducible results
7. WHEN Demo_Transactions are created, THE Audit_Logger SHALL record a demo data seeding action
8. THE Demo_Data_Generator SHALL assign realistic amounts appropriate to each transaction category

### Requirement 2: Manual Transaction Creation

**User Story:** As a user, I want to manually add transactions at any time, so that I can track all my financial activities including cash payments.

#### Acceptance Criteria

1. WHEN a User submits a Manual_Transaction with valid data, THE Transaction_Service SHALL persist the transaction to the database
2. WHEN a Manual_Transaction is created, THE Fraud_Detection_Engine SHALL analyze the transaction and compute a Fraud_Score
3. IF the Fraud_Score exceeds 70, THEN THE Fraud_Detection_Engine SHALL set the fraudulent flag to true
4. WHEN a Manual_Transaction is flagged as fraudulent, THE FinSight_System SHALL create a Fraud_Alert with appropriate severity
5. WHEN a Manual_Transaction is created, THE Audit_Logger SHALL record the transaction creation action
6. THE Transaction_Service SHALL validate that amount, type, category, and transaction date are provided before persisting
7. WHEN a Manual_Transaction creation fails validation, THE Transaction_Service SHALL return an error response with specific validation messages

### Requirement 3: Comprehensive Transaction Filtering and Sorting

**User Story:** As a user, I want to filter and sort my transactions by multiple criteria, so that I can quickly find specific transactions or analyze patterns.

#### Acceptance Criteria

1. WHEN a User requests transactions with a type filter, THE Transaction_Service SHALL return only transactions matching the specified type
2. WHEN a User requests transactions with a category filter, THE Transaction_Service SHALL return only transactions matching the specified category
3. WHEN a User requests transactions with a date range filter, THE Transaction_Service SHALL return only transactions within the specified start and end dates
4. WHEN a User requests transactions with a fraudulent filter set to true, THE Transaction_Service SHALL return only transactions flagged as fraudulent
5. WHEN a User specifies a sort field and direction, THE Transaction_Service SHALL return transactions ordered by the specified field in the specified direction
6. THE Transaction_Service SHALL support sorting by transaction date, amount, fraud score, and category
7. THE Transaction_Service SHALL support both ascending and descending sort directions
8. WHEN a User requests transactions with pagination parameters, THE Transaction_Service SHALL return the specified page of results with total count metadata
9. THE Transaction_Service SHALL support combining multiple filters simultaneously

### Requirement 4: Near Real-Time Transaction Tracking

**User Story:** As a user, I want to see my transactions update automatically, so that I can monitor my financial activity without manual page refreshes.

#### Acceptance Criteria

1. THE Frontend_Client SHALL provide a live refresh toggle control
2. WHEN the live refresh toggle is enabled, THE Frontend_Client SHALL poll the transaction endpoint every 5 to 10 seconds
3. WHEN the live refresh toggle is disabled, THE Frontend_Client SHALL stop polling the transaction endpoint
4. WHEN new transactions are detected during polling, THE Frontend_Client SHALL update the transaction list display
5. THE Transaction_Service SHALL support efficient pagination to minimize data transfer during polling requests

### Requirement 5: Rule-Based Fraud Detection

**User Story:** As a user, I want the system to automatically detect suspicious transactions, so that I can quickly identify and respond to potential fraud.

#### Acceptance Criteria

1. WHEN a Transaction amount exceeds 3 times the User's average transaction amount, THE Fraud_Detection_Engine SHALL increase the Fraud_Score
2. WHEN a User creates 5 or more transactions within a 10-minute period, THE Fraud_Detection_Engine SHALL increase the Fraud_Score for those transactions
3. WHEN a Transaction uses a category that the User has never used before, THE Fraud_Detection_Engine SHALL increase the Fraud_Score
4. WHEN a Transaction location differs from the User's recent transaction locations AND the time between transactions is less than a reasonable travel time, THE Fraud_Detection_Engine SHALL increase the Fraud_Score
5. THE Fraud_Detection_Engine SHALL compute a Fraud_Score between 0 and 100 for every transaction
6. THE Fraud_Detection_Engine SHALL assign a Risk_Level of LOW when Fraud_Score is below 40
7. THE Fraud_Detection_Engine SHALL assign a Risk_Level of MEDIUM when Fraud_Score is between 40 and 69
8. THE Fraud_Detection_Engine SHALL assign a Risk_Level of HIGH when Fraud_Score is 70 or above
9. WHEN a Transaction receives a Fraud_Score of 70 or above, THE Fraud_Detection_Engine SHALL set the fraudulent flag to true
10. WHEN a Transaction is flagged as fraudulent, THE FinSight_System SHALL create a Fraud_Alert with severity matching the Risk_Level

### Requirement 6: Fraud Alert Management

**User Story:** As a user, I want to review and resolve fraud alerts, so that I can confirm legitimate transactions and track suspicious activity.

#### Acceptance Criteria

1. WHEN a User requests fraud alerts, THE FinSight_System SHALL return all Fraud_Alerts for that User ordered by creation date descending
2. THE FinSight_System SHALL include transaction details with each Fraud_Alert in the response
3. WHEN a User resolves a Fraud_Alert, THE FinSight_System SHALL set the resolved flag to true
4. WHEN a Fraud_Alert is resolved, THE Audit_Logger SHALL record the resolution action
5. THE FinSight_System SHALL preserve all Fraud_Alerts permanently and SHALL NOT delete them
6. THE FinSight_System SHALL support filtering Fraud_Alerts by resolved status
7. THE FinSight_System SHALL support filtering Fraud_Alerts by severity level

### Requirement 7: Actionable Insights Dashboard

**User Story:** As a user, I want to see aggregated financial insights, so that I can understand my spending patterns and fraud exposure.

#### Acceptance Criteria

1. WHEN a User requests the dashboard summary, THE Dashboard_Service SHALL calculate total income from all income-type transactions
2. WHEN a User requests the dashboard summary, THE Dashboard_Service SHALL calculate total expenses from all expense-type transactions
3. WHEN a User requests the dashboard summary, THE Dashboard_Service SHALL calculate current balance as total income minus total expenses
4. WHEN a User requests the dashboard summary, THE Dashboard_Service SHALL count total flagged transactions
5. WHEN a User requests the dashboard summary, THE Dashboard_Service SHALL calculate average Fraud_Score across all transactions
6. WHEN a User requests the dashboard summary, THE Dashboard_Service SHALL aggregate spending amounts by category
7. WHEN a User requests the dashboard summary, THE Dashboard_Service SHALL aggregate fraud incidents by category
8. WHEN a User requests the dashboard summary, THE Dashboard_Service SHALL aggregate spending amounts by time period for trend analysis
9. THE Dashboard_Service SHALL support filtering summary data by date range

### Requirement 8: Subscription Detection and Tracking

**User Story:** As a user, I want the system to automatically detect recurring monthly subscriptions, so that I can track and manage my recurring expenses.

#### Acceptance Criteria

1. WHEN analyzing transactions, THE Subscription_Detector SHALL identify payments to the same merchant occurring 25 to 35 days apart
2. WHEN a merchant has 2 or more qualifying payments, THE Subscription_Detector SHALL create a Subscription record
3. THE Subscription_Detector SHALL calculate the average amount across detected subscription payments
4. THE Subscription_Detector SHALL record the last payment date for each Subscription
5. THE Subscription_Detector SHALL calculate the next due date as 30 days after the last payment date
6. WHEN a User requests subscriptions, THE FinSight_System SHALL return all detected Subscription records for that User
7. WHEN a User marks a Subscription as ignored, THE FinSight_System SHALL update the Subscription status to ignored
8. WHEN a Subscription is marked as ignored, THE Audit_Logger SHALL record the ignore action
9. WHEN a User requests due-soon subscriptions with a days parameter, THE FinSight_System SHALL return Subscriptions with next due date within the specified number of days
10. THE FinSight_System SHALL exclude ignored Subscriptions from due-soon queries

### Requirement 9: Subscription Due Notifications

**User Story:** As a user, I want to be notified about upcoming subscription payments, so that I can ensure sufficient funds are available.

#### Acceptance Criteria

1. WHEN the Frontend_Client loads, THE Frontend_Client SHALL request subscriptions due within 7 days
2. WHEN due-soon subscriptions exist, THE Frontend_Client SHALL display a notification banner with the count of upcoming payments
3. THE Frontend_Client SHALL display subscription details including merchant name, average amount, and next due date
4. WHEN a User views the subscriptions page, THE Frontend_Client SHALL highlight subscriptions due within 7 days
5. THE Frontend_Client SHALL use visual indicators to distinguish between active and ignored subscriptions

### Requirement 10: Audit Logging for Compliance

**User Story:** As a compliance officer, I want all user actions to be logged immutably, so that I can audit system activity and meet regulatory requirements.

#### Acceptance Criteria

1. WHEN a User creates a transaction, THE Audit_Logger SHALL record an audit log entry with action type, entity type, entity ID, and timestamp
2. WHEN a User resolves a Fraud_Alert, THE Audit_Logger SHALL record an audit log entry
3. WHEN a User ignores a Subscription, THE Audit_Logger SHALL record an audit log entry
4. WHEN the Demo_Data_Generator seeds transactions, THE Audit_Logger SHALL record an audit log entry
5. THE Audit_Logger SHALL include the User ID in every audit log entry
6. THE Audit_Logger SHALL include additional details in JSON format for each audit log entry
7. THE FinSight_System SHALL preserve all Audit_Log entries permanently and SHALL NOT delete or modify them
8. THE Audit_Logger SHALL record the timestamp of each action in UTC

### Requirement 11: Error Handling and Response Standards

**User Story:** As a developer, I want consistent error responses across all endpoints, so that I can handle errors predictably in the frontend.

#### Acceptance Criteria

1. WHEN a request fails validation, THE FinSight_System SHALL return HTTP status code 400 with error details
2. WHEN a requested resource is not found, THE FinSight_System SHALL return HTTP status code 404 with error details
3. WHEN an internal error occurs, THE FinSight_System SHALL return HTTP status code 500 with a generic error message
4. THE FinSight_System SHALL return error responses in a consistent JSON format with message and timestamp fields
5. WHEN multiple validation errors occur, THE FinSight_System SHALL return all validation errors in a single response
6. THE FinSight_System SHALL log all errors with sufficient detail for debugging

### Requirement 12: Test Data Determinism

**User Story:** As a developer, I want demo data generation to be deterministic, so that I can write reliable automated tests.

#### Acceptance Criteria

1. WHEN the Demo_Data_Generator is invoked with the same User ID multiple times, THE Demo_Data_Generator SHALL produce identical transaction sets
2. THE Demo_Data_Generator SHALL use a seeded random number generator initialized with the User ID
3. WHEN testing fraud detection rules, THE FinSight_System SHALL produce consistent Fraud_Scores for identical transaction patterns
4. THE Subscription_Detector SHALL produce consistent results when analyzing the same transaction set multiple times

### Requirement 13: Docker Deployment

**User Story:** As a developer, I want to run the entire application with a single command, so that I can quickly set up development and demo environments.

#### Acceptance Criteria

1. THE FinSight_System SHALL provide a docker-compose configuration that starts both backend and frontend services
2. WHEN a User runs docker compose up with the build flag, THE FinSight_System SHALL build and start all required containers
3. THE FinSight_System SHALL use Docker images compatible with Apple Silicon architecture
4. THE FinSight_System SHALL expose the backend API on a documented port
5. THE FinSight_System SHALL expose the frontend application on a documented port
6. THE FinSight_System SHALL include health check endpoints for container orchestration

### Requirement 14: Frontend User Interface

**User Story:** As a user, I want a clean and responsive interface, so that I can easily manage my transactions and review fraud alerts on any device.

#### Acceptance Criteria

1. THE Frontend_Client SHALL provide a login and registration page
2. THE Frontend_Client SHALL provide a transactions page with filtering controls and manual transaction creation
3. THE Frontend_Client SHALL provide a dashboard page with summary cards and charts
4. THE Frontend_Client SHALL provide a fraud alerts page with resolution controls
5. THE Frontend_Client SHALL provide a subscriptions page with due-soon notifications
6. THE Frontend_Client SHALL use responsive CSS that adapts to mobile and desktop screen sizes
7. THE Frontend_Client SHALL display Risk_Level badges with color coding for LOW, MEDIUM, and HIGH
8. THE Frontend_Client SHALL display status badges for resolved and unresolved Fraud_Alerts
9. THE Frontend_Client SHALL display toast notifications for successful actions and errors
10. THE Frontend_Client SHALL display a banner notification when subscriptions are due within 7 days

### Requirement 15: API Backward Compatibility

**User Story:** As a developer, I want existing API endpoints to continue working, so that I don't break any existing integrations or frontend code.

#### Acceptance Criteria

1. THE FinSight_System SHALL maintain all existing endpoint paths and HTTP methods
2. THE FinSight_System SHALL maintain all existing request and response data structures
3. WHEN existing endpoints are extended with new optional parameters, THE FinSight_System SHALL maintain backward compatibility by using default values
4. THE FinSight_System SHALL continue to support existing authentication and authorization mechanisms
