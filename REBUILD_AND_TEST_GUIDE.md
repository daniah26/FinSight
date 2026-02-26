# Rebuild and Test Guide - Multi-Month Transactions

## Current Status
The code has been updated to generate transactions across 12 months (going back from current date). However, you need to properly rebuild and restart to see the changes.

## Step-by-Step Instructions

### 1. Stop Any Running Containers
```bash
docker-compose down
```

### 2. Rebuild with No Cache (Important!)
```bash
docker-compose build --no-cache
```
This ensures the new code is compiled and packaged.

### 3. Start the Containers
```bash
docker-compose up
```

### 4. Wait for Backend to Start
Look for these log lines:
- "Started FinSightApplication"
- "Tomcat started on port 8080"

### 5. Reseed Demo Data
```bash
curl -X POST "http://localhost:8080/api/transactions/reseed-demo?userId=1"
```

### 6. Check the Logs
In the Docker logs, you should now see:
```
Starting demo transaction generation. Current date: 2026-02-26...
Generating 12 transactions for month: MARCH 2025
Generating 14 transactions for month: APRIL 2025
Generating 15 transactions for month: MAY 2025
...
Generating 30 transactions for month: FEBRUARY 2026
Generated 246 total transactions across 12 months
```

### 7. Verify Date Distribution
```bash
curl "http://localhost:8080/api/transactions/debug/date-distribution?userId=1"
```

Expected output should show transactions across 12 months:
```json
{
  "totalTransactions": 246,
  "monthDistribution": {
    "2025-03": 12,
    "2025-04": 14,
    "2025-05": 15,
    "2025-06": 16,
    "2025-07": 18,
    "2025-08": 20,
    "2025-09": 22,
    "2025-10": 24,
    "2025-11": 25,
    "2025-12": 26,
    "2026-01": 28,
    "2026-02": 30
  }
}
```

### 8. Check Frontend
Open http://localhost:3000 and verify:
- Dashboard shows transactions across multiple months
- Transaction charts display data for all 12 months
- Transaction page shows dates from March 2025 to February 2026

## Troubleshooting

### If logs still don't show "Starting demo transaction generation"
- The old code is still running
- Try: `docker-compose down -v` (removes volumes)
- Then rebuild: `docker-compose build --no-cache`

### If debug endpoint shows only Feb 2026
- Backend didn't rebuild properly
- Check Docker build logs for compilation errors
- Verify the DemoDataService.java file has the 12-month loop

### If frontend shows only recent months
- This is a frontend filtering issue, not a backend issue
- Check the date range filters in the frontend
- The backend data is correct if the debug endpoint shows all months

## What Changed in the Code

### DemoDataService - Multi-Month Transactions
The `generateDemoTransactions` method now:
1. Generates transactions across 12 months (not just current month)
2. Goes back 11 months from current date
3. Creates an increasing number of transactions per month (12 to 30)
4. Logs each month being generated
5. Does NOT overwrite dates in fraud scenarios

### SubscriptionDetectorService - Strict Monthly Detection
The subscription detection now enforces:
1. **ONE transaction per month rule**: If a category has multiple transactions in ANY month, it's NOT considered a subscription
2. At least 3 transactions total to establish a pattern
3. Transactions must be 25-35 days apart (monthly pattern)
4. At least 2 consecutive monthly occurrences
5. This ensures only true monthly recurring subscriptions are detected (like Netflix, Spotify)
6. Categories with multiple purchases per month (like groceries, gas) are excluded

### Fraud Detection Logic
- Only marks EXPENSE transactions with score >= 40 as fraudulent
- INCOME transactions are never marked as fraudulent
- Fraud alerts only created for MEDIUM (40-69) and HIGH (70-100) severity
