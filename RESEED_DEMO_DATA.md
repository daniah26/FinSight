# Reseed Demo Data with Fraud Alerts - FIXED

## Issue Fixed

The demo transactions were being created but fraud alerts weren't showing up. This was because:
1. Fraud alerts weren't being created in the demo data service
2. The order of operations was incorrect

## What Was Fixed

1. Added `FraudAlertRepository` to `DemoDataService`
2. Modified `seedUser()` to:
   - Save each transaction individually
   - Run fraud detection immediately after saving
   - Create fraud alerts for any transaction with fraud score > 0
   - Update transaction with fraud scores
3. Modified `forceReseedUser()` to delete existing fraud alerts before transactions
4. Added `createFraudAlert()` method to create alerts with proper severity levels

## What Was Added

I've enhanced the demo data service to create **5 different fraud scenarios** that will trigger fraud alerts:

### Fraud Scenarios

1. **Medium Risk (50 points)** - High amount + unusual category
   - $5,000-$10,000 purchase in "luxury_electronics"
   - Triggers: High amount (30 pts) + Unusual category (20 pts)

2. **Low Risk (25 points)** - Rapid-fire activity
   - 5 transactions within 10 minutes
   - Triggers: Rapid-fire detection (25 pts)

3. **High Fraud (75 points)** - Multiple triggers
   - 5 rapid crypto transactions with high amounts ($3,000-$5,000 each)
   - Triggers: High amount (30 pts) + Rapid-fire (25 pts) + Unusual category (20 pts)

4. **Medium Risk (55 points)** - Geographical anomaly
   - Purchase in New York, then Los Angeles 30 minutes later with $4,000 amount
   - Triggers: Geographical anomaly (25 pts) + High amount (30 pts)

5. **Extreme Fraud (100 points)** - All rules triggered
   - 5 rapid offshore transfers ($8,000-$10,000 each) in Tokyo within minutes of a Chicago transaction
   - Triggers: All rules (30 + 25 + 25 + 20 = 100 pts)

## How to Reseed Demo Data

### Option 1: Using curl (Command Line)

Replace `YOUR_USER_ID` with your actual user ID (usually 1 for the first user):

```bash
curl -X POST "http://localhost:8080/api/transactions/reseed-demo?userId=YOUR_USER_ID"
```

Example:
```bash
curl -X POST "http://localhost:8080/api/transactions/reseed-demo?userId=1"
```

### Option 2: Using PowerShell (Windows)

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/transactions/reseed-demo?userId=1" -Method POST
```

### Option 3: Using Browser Console

1. Open your browser's Developer Tools (F12)
2. Go to the Console tab
3. Run this JavaScript:

```javascript
fetch('http://localhost:8080/api/transactions/reseed-demo?userId=1', {
  method: 'POST'
})
.then(response => response.json())
.then(data => console.log('Success:', data))
.catch(error => console.error('Error:', error));
```

### Option 4: Create a New User

Simply sign up with a new account - the fraud scenarios will be automatically created during signup!

## What Happens

When you reseed:
1. All existing transactions for the user are deleted
2. 35-50 new demo transactions are created (increased from 25-50)
3. The 5 fraud scenarios are automatically added
4. Fraud detection runs on all transactions
5. Fraud alerts are created for transactions with score >= 70

## Expected Results

After reseeding, you should see:
- Multiple fraud alerts in the Fraud Alerts page
- Transactions with HIGH and CRITICAL risk levels
- Detailed fraud reasons for each flagged transaction
- Dashboard showing fraud statistics

## Files Modified

1. `backend/src/main/java/com/finsight/service/DemoDataService.java`
   - Enhanced `addFraudTriggers()` with 5 detailed fraud scenarios
   - Added `forceReseedUser()` method to delete and recreate transactions
   - Increased transaction count to 35-50 to ensure all scenarios fit

2. `backend/src/main/java/com/finsight/controller/TransactionController.java`
   - Added `/api/transactions/reseed-demo` POST endpoint
   - Accepts `userId` query parameter

## Troubleshooting

If you don't see fraud alerts after reseeding:
1. Check the backend logs for fraud detection output
2. Verify the user ID is correct
3. Refresh the frontend page
4. Check the Fraud Alerts page (not just Dashboard)
