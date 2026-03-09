# Reseed Demo Data Instructions

The demo transaction generation code has been updated to spread transactions across 4 months (November 2025 - February 2026) instead of clustering them in recent dates.

## To Apply the Changes

You need to reseed the demo data for your user account. Here are three ways to do it:

### Option 1: Using curl (Recommended)
```bash
curl -X POST "http://localhost:8080/api/transactions/reseed-demo?userId=1"
```

### Option 2: Using PowerShell
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/transactions/reseed-demo?userId=1" -Method POST
```

### Option 3: Using your browser
1. Open your browser's developer console (F12)
2. Go to the Console tab
3. Paste and run:
```javascript
fetch('http://localhost:8080/api/transactions/reseed-demo?userId=1', { method: 'POST' })
  .then(r => r.json())
  .then(d => console.log(d));
```

## What This Does

The reseed endpoint will:
1. Delete all existing transactions and fraud alerts for the user
2. Generate new demo transactions spread across 4 months:
   - **November 2025**: 10 transactions (including 1 fraud scenario)
   - **December 2025**: 12 transactions (including 1 fraud scenario)
   - **January 2026**: 14 transactions (including 3 fraud scenarios)
   - **February 2026**: 15 transactions (including 3 fraud scenarios)
3. Run fraud detection on all new transactions
4. Create fraud alerts for suspicious transactions

## Expected Results

After reseeding, you should see:
- **51 total transactions** spread across 4 months
- Transactions with varying dates throughout each month
- **Multiple fraud alerts** distributed across different months
- Better data for testing trends, analytics, and monthly comparisons

## Verify the Changes

1. Refresh your dashboard
2. Check the Transactions page - you should see dates from November 2025 onwards
3. Look at spending trends - they should show data across multiple months
4. Check fraud alerts - they should appear in different months

## Note

Replace `userId=1` with your actual user ID if different. The demo user typically has ID 1.
