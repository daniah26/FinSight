# 📱 Subscription Detection Fix

## Issues Fixed

### 1. Detection Too Strict
**Problem:** Required at least 2 recurring patterns (3+ transactions), making it hard to test
**Fix:** Now requires only 1 recurring pattern (2 transactions)

### 2. Time Window Too Narrow
**Problem:** Only detected transactions 25-35 days apart
**Fix:** Expanded to 20-40 days for more flexibility

### 3. Duplicate Subscriptions
**Problem:** Created duplicate subscriptions on each page load
**Fix:** Now clears existing subscriptions before detecting new ones

### 4. Merchant Matching Too Strict
**Problem:** "Netflix" and "Netflix Subscription" wouldn't match
**Fix:** Improved normalization to extract key merchant name

### 5. Multiple Payments Same Month (NEW)
**Problem:** Multiple payments to same merchant within a month created multiple subscription entries
**Fix:** Filters out transactions less than 20 days apart, keeping only the true monthly pattern

## Changes Made

### File: `SubscriptionDetectorService.java`

**1. Clear existing subscriptions before detection:**
```java
// Clear existing subscriptions for this user to avoid duplicates
List<Subscription> existingSubs = subscriptionRepository.findByUser(user);
subscriptionRepository.deleteAll(existingSubs);
```

**2. More lenient time window:**
```java
// More lenient: 20-40 days (roughly monthly)
if (daysBetween >= 20 && daysBetween <= 40) {
    recurringCount++;
}
```

**3. Lower threshold:**
```java
// Need at least 1 recurring pattern (2 transactions ~30 days apart)
if (recurringCount >= 1) {
    // Create subscription
}
```

**4. Better merchant normalization:**
```java
// Extract first significant word (usually the merchant name)
String[] words = normalized.split("\\s+");
if (words.length > 0 && words[0].length() >= 3) {
    return words[0];
}
```

**5. Transaction filtering:**
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
```

**6. Enhanced logging:**
```java
log.info("Detected subscription for user {}: merchant={}, avgAmount={}, nextDue={}", 
    userId, sub.getMerchant(), sub.getAvgAmount(), sub.getNextDueDate());
```

## How to Test

### Quick Test (2 Transactions)

1. **Create Transaction 1:**
   - Amount: $15.99
   - Type: EXPENSE
   - Category: Entertainment
   - Description: Netflix
   - Date: 30 days ago (or use current date and manually adjust)

2. **Create Transaction 2:**
   - Amount: $15.99
   - Type: EXPENSE
   - Category: Entertainment
   - Description: Netflix
   - Date: Today

3. **Go to Subscriptions page**
   - Should see Netflix subscription detected
   - Average amount: $15.99
   - Next due date: 30 days from today

### Test Due Soon Feature

1. **Create Transaction 1:**
   - Amount: $9.99
   - Type: EXPENSE
   - Category: Entertainment
   - Description: Spotify
   - Date: 53 days ago

2. **Create Transaction 2:**
   - Amount: $9.99
   - Type: EXPENSE
   - Category: Entertainment
   - Description: Spotify
   - Date: 23 days ago

3. **Go to Subscriptions page**
   - Should see Spotify subscription
   - Next due date: 7 days from now
   - Should appear in "Due Soon" banner

## Rebuild and Test

### Rebuild Backend
```bash
# Stop containers
docker-compose down

# Rebuild backend with fixes
docker-compose up --build backend
```

### Verify in Logs
```bash
# Watch for subscription detection logs
docker logs -f finsight-backend | grep "Detected subscription"
```

You should see:
```
Detected subscription for user 1: merchant=Netflix, avgAmount=15.99, nextDue=2024-XX-XX
```

### Test in Browser

1. Open http://localhost:3000
2. Go to Transactions page
3. Create 2 expense transactions with same description, 30 days apart
4. Go to Subscriptions page
5. Should see the subscription detected!

## What Changed

### Before
- ❌ Required 3+ transactions (2+ recurring patterns)
- ❌ Only detected 25-35 day intervals
- ❌ Created duplicate subscriptions
- ❌ Strict merchant name matching
- ❌ Hard to test

### After
- ✅ Requires only 2 transactions (1 recurring pattern)
- ✅ Detects 20-40 day intervals (more flexible)
- ✅ Clears old subscriptions before detecting
- ✅ Smart merchant name matching
- ✅ Filters duplicate payments within 20 days
- ✅ Creates ONE subscription per merchant
- ✅ Easy to test

## Detection Logic Summary

```
For each merchant:
1. Find all EXPENSE transactions
2. Group by normalized merchant name
3. Sort by date
4. Check if any 2 consecutive transactions are 20-40 days apart
5. If yes, create subscription:
   - Average amount from all transactions
   - Last payment date = most recent transaction
   - Next due date = last payment + 30 days
   - Status = ACTIVE
```

## Due Soon Logic

```
Subscriptions are "Due Soon" when:
- Status = ACTIVE (not IGNORED)
- Next due date is between today and 7 days from now
```

## Tips for Testing

1. **Use consistent descriptions** - "Netflix" for all Netflix transactions
2. **Space transactions ~30 days apart** - This is the ideal interval
3. **Use EXPENSE type** - Subscriptions are expenses
4. **Refresh the page** - Detection runs when you visit Subscriptions page
5. **Check the logs** - See what's being detected in backend logs

## Common Test Scenarios

### Scenario 1: Basic Detection
```
Transaction 1: Netflix, $15.99, 30 days ago
Transaction 2: Netflix, $15.99, Today
Result: Netflix subscription detected
```

### Scenario 2: Variable Amounts
```
Transaction 1: Electric Bill, $120, 30 days ago
Transaction 2: Electric Bill, $135, Today
Result: Electric Bill subscription, avg $127.50
```

### Scenario 3: Multiple Subscriptions
```
Netflix: 2 transactions, 30 days apart
Spotify: 2 transactions, 30 days apart
Gym: 2 transactions, 35 days apart
Result: 3 subscriptions detected
```

### Scenario 4: Due Soon
```
Transaction 1: Hulu, $12.99, 53 days ago
Transaction 2: Hulu, $12.99, 23 days ago
Result: Hulu subscription, due in 7 days (shows in banner)
```

## Documentation

See `SUBSCRIPTION_TESTING_GUIDE.md` for comprehensive testing instructions and examples.

## Summary

The subscription detection system is now more flexible and easier to test:
- ✅ Requires only 2 transactions instead of 3+
- ✅ Accepts 20-40 day intervals instead of 25-35
- ✅ No more duplicate subscriptions
- ✅ Better merchant name matching
- ✅ Enhanced logging for debugging

Rebuild the backend and test with 2 transactions from the same merchant! 🚀
