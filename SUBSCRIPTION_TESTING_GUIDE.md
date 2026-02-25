# 📱 Subscription Detection Testing Guide

## How Subscription Detection Works

The system automatically detects recurring payments by analyzing your transaction history. Here's how it works:

### Detection Algorithm

1. **Groups transactions by merchant** - Uses the description field to identify the same merchant
2. **Looks for recurring patterns** - Finds transactions 20-40 days apart (roughly monthly)
3. **Requires minimum 2 transactions** - At least 2 transactions from the same merchant
4. **Calculates average amount** - Averages all transactions from that merchant
5. **Predicts next due date** - Last payment date + 30 days

### Requirements for Detection

- **Minimum**: 2 expense transactions
- **Same merchant**: Description should be similar (e.g., "Netflix", "Netflix Subscription")
- **Time gap**: 20-40 days between transactions (flexible monthly pattern)
- **Transaction type**: Must be EXPENSE (not INCOME)

## Testing Subscription Detection

### Example 1: Netflix Subscription

Create these transactions to test:

**Transaction 1:**
- Amount: $15.99
- Type: EXPENSE
- Category: Entertainment
- Description: Netflix
- Date: 30 days ago

**Transaction 2:**
- Amount: $15.99
- Type: EXPENSE
- Category: Entertainment
- Description: Netflix
- Date: Today

**Result:** Should detect Netflix subscription with:
- Average amount: $15.99
- Next due date: 30 days from today
- Status: ACTIVE

### Example 2: Spotify Subscription

**Transaction 1:**
- Amount: $9.99
- Type: EXPENSE
- Category: Entertainment
- Description: Spotify Premium
- Date: 60 days ago

**Transaction 2:**
- Amount: $9.99
- Type: EXPENSE
- Category: Entertainment
- Description: Spotify Premium
- Date: 30 days ago

**Transaction 3:**
- Amount: $9.99
- Type: EXPENSE
- Category: Entertainment
- Description: Spotify Premium
- Date: Today

**Result:** Should detect Spotify subscription with:
- Average amount: $9.99
- Next due date: 30 days from today
- Multiple recurring patterns detected

### Example 3: Gym Membership

**Transaction 1:**
- Amount: $50.00
- Type: EXPENSE
- Category: Health
- Description: Planet Fitness
- Date: 35 days ago

**Transaction 2:**
- Amount: $50.00
- Type: EXPENSE
- Category: Health
- Description: Planet Fitness
- Date: Today

**Result:** Should detect gym subscription (35 days is within 20-40 day range)

## Due Soon Notifications

Subscriptions are marked as "Due Soon" when:
- Next due date is within 7 days
- Status is ACTIVE (not IGNORED)

### Testing Due Soon

To test the due soon feature:

1. Create 2 transactions from the same merchant
2. Make the second transaction 23 days ago
3. The next due date will be in 7 days (23 + 30 = 53 days from first transaction)
4. The subscription should appear in the "Due Soon" banner

**Example:**

**Transaction 1:**
- Description: Adobe Creative Cloud
- Amount: $54.99
- Date: 53 days ago

**Transaction 2:**
- Description: Adobe Creative Cloud
- Amount: $54.99
- Date: 23 days ago

**Result:** Next due date = 7 days from now → Shows in "Due Soon" banner

## Quick Test Scenarios

### Scenario 1: Create Multiple Subscriptions

```
1. Netflix - $15.99 - 30 days ago
2. Netflix - $15.99 - Today
3. Spotify - $9.99 - 30 days ago
4. Spotify - $9.99 - Today
5. Amazon Prime - $14.99 - 30 days ago
6. Amazon Prime - $14.99 - Today
```

**Expected:** 3 subscriptions detected

### Scenario 2: Test Due Soon

```
1. Hulu - $12.99 - 23 days ago
2. Hulu - $12.99 - Today (but change date to 23 days ago in transaction)
```

**Expected:** Hulu subscription due in 7 days

### Scenario 3: Variable Amounts

```
1. Electric Bill - $120.00 - 30 days ago
2. Electric Bill - $135.00 - Today
```

**Expected:** Electric Bill subscription with average $127.50

## Merchant Name Matching

The system normalizes merchant names for matching:

### These Will Match:
- "Netflix" and "Netflix Subscription"
- "Spotify" and "Spotify Premium"
- "Amazon Prime" and "Amazon Prime Video"

### These Won't Match:
- "Netflix" and "Hulu" (different merchants)
- "Gym" and "Fitness Center" (different first words)

**Tip:** Use consistent descriptions for the same merchant!

## Common Issues

### Issue: No Subscriptions Detected

**Possible Causes:**
1. Less than 2 transactions from the same merchant
2. Transactions more than 40 days apart or less than 20 days apart
3. Transactions are INCOME type (should be EXPENSE)
4. Merchant descriptions are too different

**Solution:**
- Create at least 2 expense transactions
- Use similar descriptions
- Space them 25-35 days apart for best results

### Issue: Duplicate Subscriptions

**Cause:** The system recreates subscriptions each time you view the page

**Solution:** This is by design - it analyzes current transaction history each time

### Issue: Wrong Next Due Date

**Cause:** System uses last transaction + 30 days

**Solution:** This is expected behavior - it assumes monthly billing

## Ignoring Subscriptions

If a detected subscription is not actually a subscription:

1. Click the "Ignore" button on the subscription card
2. Status changes to IGNORED
3. Won't appear in "Due Soon" notifications
4. Still visible in the list but grayed out

## API Endpoints

### Get All Subscriptions
```
GET /api/subscriptions?userId=1
```
Detects and returns all subscriptions

### Get Due Soon
```
GET /api/subscriptions/due-soon?userId=1&days=7
```
Returns subscriptions due within 7 days

### Ignore Subscription
```
PUT /api/subscriptions/{id}/ignore?userId=1
```
Marks subscription as ignored

## Tips for Best Results

1. **Use consistent merchant names** - "Netflix" not "Netflix Inc." or "NETFLIX"
2. **Create transactions ~30 days apart** - This is the sweet spot
3. **Use EXPENSE type** - Subscriptions are expenses, not income
4. **Be patient** - Detection runs when you visit the Subscriptions page
5. **Test with real dates** - Use actual dates 30 days apart, not just "today"

## Example Test Data Set

Here's a complete test data set you can use:

```javascript
// Create these transactions in order:

// Netflix - Monthly
1. Amount: $15.99, Type: EXPENSE, Category: Entertainment, 
   Description: "Netflix", Date: 60 days ago

2. Amount: $15.99, Type: EXPENSE, Category: Entertainment,
   Description: "Netflix", Date: 30 days ago

// Spotify - Due Soon
3. Amount: $9.99, Type: EXPENSE, Category: Entertainment,
   Description: "Spotify", Date: 23 days ago

4. Amount: $9.99, Type: EXPENSE, Category: Entertainment,
   Description: "Spotify", Date: Today (but set to 23 days ago)

// Gym - Variable Amount
5. Amount: $45.00, Type: EXPENSE, Category: Health,
   Description: "Gym Membership", Date: 35 days ago

6. Amount: $50.00, Type: EXPENSE, Category: Health,
   Description: "Gym Membership", Date: Today
```

**Expected Results:**
- 3 subscriptions detected
- Spotify shows as "Due Soon" (7 days)
- Netflix next due in 30 days
- Gym next due in 30 days

## Troubleshooting

### Check Backend Logs
```bash
docker logs finsight-backend | grep "Detected subscription"
```

Should show:
```
Detected subscription for user 1: merchant=Netflix, avgAmount=15.99, nextDue=2024-XX-XX
```

### Verify Transactions
Make sure your transactions:
- Have userId=1
- Are type=EXPENSE
- Have similar descriptions
- Are 20-40 days apart

### Refresh the Page
The detection runs each time you visit the Subscriptions page, so refresh to see updates.

## Summary

The subscription detection system is designed to automatically identify recurring payments from your transaction history. For best results:

✅ Create at least 2 transactions per merchant
✅ Space them 25-35 days apart
✅ Use consistent merchant names
✅ Use EXPENSE type
✅ Refresh the Subscriptions page to see results

Happy testing! 🎉
