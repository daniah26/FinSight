# Demo Data Future Date Fix

## Problem
The demo data generator was creating transactions with future dates. For example, on March 8, 2026, it would generate transactions dated March 30, 2026.

## Root Cause
The `generateDemoTransactions` method generates transactions across 12 months by:
1. Going back 11 months from the current month
2. Randomly selecting days within each month
3. Not checking if the generated date is in the future

For example, on March 8, 2026:
- Current month: March 2026
- Generates transactions for March 2026 (monthsBack = 0)
- Randomly picks days 1-31
- Days 9-31 are in the future!

## Fix Applied

Added a check to skip any transaction dates that would be in the future:

```java
LocalDateTime txnDate = monthStart
    .withDayOfMonth(dayOfMonth)
    .withHour(hour)
    .withMinute(minute);

// Skip transactions that would be in the future
if (txnDate.isAfter(now)) {
    log.debug("Skipping future transaction date: {}", txnDate);
    continue;
}
```

## How It Works Now

**Before Fix:**
- Current date: March 8, 2026
- Generates transactions: March 1-31, 2026
- Result: Transactions dated March 9-31 are in the future ❌

**After Fix:**
- Current date: March 8, 2026
- Generates transactions: March 1-31, 2026
- Skips: March 9-31 (future dates)
- Result: Only transactions dated March 1-8 are created ✓

## Impact

1. **New Users:**
   - Demo data will only contain past transactions
   - No future-dated transactions

2. **Existing Users:**
   - Need to reseed demo data to fix existing future transactions
   - Use the reseed endpoint: `POST /api/transactions/reseed-demo?userId={userId}`

3. **Subscription Detection:**
   - Will work correctly with only past transactions
   - No false "due soon" alerts from future transactions

## Testing

1. **Create a new user:**
   - Sign up with a new account
   - Verify all demo transactions are dated today or earlier
   - Check Transactions page - no future dates
   - Check Dashboard - all dates are valid

2. **Reseed existing user:**
   ```bash
   curl -X POST http://localhost:8080/api/transactions/reseed-demo?userId=1
   ```
   - Verify all transactions are now dated today or earlier
   - Check subscriptions are detected correctly

3. **Edge case - end of month:**
   - Test on March 31
   - Verify transactions for March 1-31 are all created
   - Test on April 1
   - Verify no March transactions are in the future

## Files Modified

- `backend/src/main/java/com/finsight/service/DemoDataService.java` - Added future date check

## Notes

- The fix is simple and effective - just skip future dates
- Slightly fewer transactions will be generated in the current month
- This is expected behavior and doesn't affect the demo experience
- The transaction count may vary slightly depending on the current day of the month
- Fraud scenarios are still properly distributed across the generated transactions
