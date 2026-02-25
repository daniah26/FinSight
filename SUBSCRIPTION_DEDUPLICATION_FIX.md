# 🔄 Subscription Deduplication Fix

## Problem

When multiple payments were made to the same merchant within a short time period (e.g., within the same month), the system was creating multiple subscription entries instead of recognizing them as part of the same subscription.

### Example of the Problem

**Scenario:**
```
Transaction 1: Netflix - $15.99 - Jan 1
Transaction 2: Netflix - $15.99 - Jan 15 (accidental duplicate payment)
Transaction 3: Netflix - $15.99 - Feb 1
```

**Old Behavior:**
- Created 2 separate Netflix subscriptions
- One for Jan 1 → Jan 15 (15 days apart - not detected)
- One for Jan 15 → Feb 1 (17 days apart - not detected)
- Or worse, detected multiple overlapping subscriptions

**Desired Behavior:**
- Create 1 Netflix subscription
- Use Jan 1 and Feb 1 as the recurring pattern (31 days apart)
- Ignore the Jan 15 payment as it's too close to Jan 1

## Solution

Added a **filtering step** that removes transactions that are too close together (less than 20 days apart) before analyzing for recurring patterns.

### How It Works

1. **Group by merchant** - All transactions with similar descriptions
2. **Sort by date** - Oldest to newest
3. **Filter close transactions** - Keep only transactions that are at least 20 days apart
4. **Detect pattern** - Look for recurring pattern in filtered transactions
5. **Create ONE subscription** - Single subscription per merchant

### Algorithm

```java
// Start with first transaction
filteredTxns.add(txns.get(0));

// For each subsequent transaction
for (transaction in remaining_transactions) {
    daysSinceLast = days_between(last_filtered_transaction, transaction);
    
    // Only include if at least 20 days since last included transaction
    if (daysSinceLast >= 20) {
        filteredTxns.add(transaction);
    }
}

// Now analyze filtered transactions for recurring pattern
```

## Examples

### Example 1: Duplicate Payment in Same Month

**Transactions:**
```
1. Netflix - $15.99 - Jan 1
2. Netflix - $15.99 - Jan 15 (duplicate/mistake)
3. Netflix - $15.99 - Feb 1
```

**Processing:**
1. Sort by date: Jan 1, Jan 15, Feb 1
2. Filter:
   - Include Jan 1 (first transaction)
   - Skip Jan 15 (only 14 days since Jan 1, less than 20)
   - Include Feb 1 (31 days since Jan 1, more than 20)
3. Filtered list: Jan 1, Feb 1
4. Check pattern: 31 days apart ✅ (within 20-40 range)
5. Create ONE subscription: Netflix, $15.99/month, next due Mar 1

**Result:** ✅ Single Netflix subscription detected

### Example 2: Multiple Payments in One Month

**Transactions:**
```
1. Spotify - $9.99 - Jan 1
2. Spotify - $9.99 - Jan 10 (family member paid by mistake)
3. Spotify - $9.99 - Jan 20 (another duplicate)
4. Spotify - $9.99 - Feb 1
```

**Processing:**
1. Sort: Jan 1, Jan 10, Jan 20, Feb 1
2. Filter:
   - Include Jan 1 (first)
   - Skip Jan 10 (9 days since Jan 1)
   - Skip Jan 20 (19 days since Jan 1, still less than 20)
   - Include Feb 1 (31 days since Jan 1)
3. Filtered: Jan 1, Feb 1
4. Pattern: 31 days ✅
5. Create ONE subscription

**Result:** ✅ Single Spotify subscription, ignores duplicates

### Example 3: Legitimate Bi-Weekly Payments

**Transactions:**
```
1. Gym - $50 - Jan 1
2. Gym - $50 - Jan 15
3. Gym - $50 - Feb 1
```

**Processing:**
1. Sort: Jan 1, Jan 15, Feb 1
2. Filter:
   - Include Jan 1 (first)
   - Skip Jan 15 (14 days, less than 20)
   - Include Feb 1 (31 days since Jan 1)
3. Filtered: Jan 1, Feb 1
4. Pattern: 31 days ✅
5. Create ONE subscription

**Result:** ✅ Single Gym subscription (treats as monthly, not bi-weekly)

**Note:** The system is designed for monthly subscriptions. Bi-weekly payments will be detected as monthly.

### Example 4: True Monthly Subscription

**Transactions:**
```
1. Amazon Prime - $14.99 - Jan 1
2. Amazon Prime - $14.99 - Feb 1
3. Amazon Prime - $14.99 - Mar 1
```

**Processing:**
1. Sort: Jan 1, Feb 1, Mar 1
2. Filter:
   - Include Jan 1 (first)
   - Include Feb 1 (31 days since Jan 1)
   - Include Mar 1 (28 days since Feb 1)
3. Filtered: Jan 1, Feb 1, Mar 1 (all included)
4. Pattern: Multiple recurring patterns ✅
5. Create ONE subscription

**Result:** ✅ Single Amazon Prime subscription with strong pattern

## Key Changes

### Before
```java
// Analyzed ALL transactions
for (int i = 1; i < txns.size(); i++) {
    long daysBetween = ChronoUnit.DAYS.between(
        txns.get(i-1).getTransactionDate(),
        txns.get(i).getTransactionDate()
    );
    if (daysBetween >= 20 && daysBetween <= 40) {
        recurringCount++;
    }
}
```

**Problem:** Consecutive transactions could be very close together, creating false patterns or multiple subscriptions.

### After
```java
// Filter out transactions less than 20 days apart
List<Transaction> filteredTxns = new ArrayList<>();
filteredTxns.add(txns.get(0));

for (int i = 1; i < txns.size(); i++) {
    long daysSinceLast = ChronoUnit.DAYS.between(
        filteredTxns.get(filteredTxns.size() - 1).getTransactionDate(),
        txns.get(i).getTransactionDate()
    );
    
    if (daysSinceLast >= 20) {
        filteredTxns.add(txns.get(i));
    }
}

// Then analyze filtered transactions
```

**Solution:** Only compares transactions that are at least 20 days apart, ensuring we detect the true monthly pattern.

## Benefits

1. **No Duplicate Subscriptions** - One subscription per merchant, regardless of extra payments
2. **Handles Mistakes** - Ignores accidental duplicate payments within the same month
3. **Cleaner Detection** - Focuses on the true recurring pattern
4. **Better User Experience** - Users see one subscription entry per service
5. **Accurate Predictions** - Next due date based on actual monthly pattern

## Testing

### Test Case 1: Duplicate Payment
```
1. Create: Netflix, $15.99, 30 days ago
2. Create: Netflix, $15.99, 15 days ago (duplicate)
3. Create: Netflix, $15.99, today
4. Go to Subscriptions page
5. Expected: ONE Netflix subscription
```

### Test Case 2: Multiple Duplicates
```
1. Create: Spotify, $9.99, 60 days ago
2. Create: Spotify, $9.99, 50 days ago (duplicate)
3. Create: Spotify, $9.99, 30 days ago
4. Create: Spotify, $9.99, 20 days ago (duplicate)
5. Create: Spotify, $9.99, today
6. Expected: ONE Spotify subscription
```

### Test Case 3: Normal Monthly
```
1. Create: Gym, $50, 60 days ago
2. Create: Gym, $50, 30 days ago
3. Create: Gym, $50, today
4. Expected: ONE Gym subscription with strong pattern
```

## Rebuild and Test

```bash
# Rebuild backend
docker-compose down
docker-compose up --build backend
```

## Verification

Check the logs to see the filtering in action:

```bash
docker logs -f finsight-backend | grep "Detected subscription"
```

You should see:
```
Detected subscription for user 1: merchant=Netflix, avgAmount=15.99, 
nextDue=2024-XX-XX, transactionCount=2
```

The `transactionCount` shows how many transactions were used after filtering.

## Summary

The subscription detection now:
- ✅ Creates ONE subscription per merchant
- ✅ Filters out duplicate payments within 20 days
- ✅ Focuses on true monthly recurring patterns
- ✅ Handles accidental duplicate payments gracefully
- ✅ Provides cleaner, more accurate subscription detection

No more duplicate subscription entries! 🎉
