# 🚨 Fraud Detection Testing Guide

## Overview

The fraud detection system uses 4 rule-based algorithms to analyze transactions and assign fraud scores from 0-100. This guide explains how each rule works and how to test them.

## Fraud Detection Rules

### Rule 1: High Amount Anomaly (+30 points)
**Triggers when:** Transaction amount > 3× user's average transaction amount

**Example:**
```
User's average transaction: $50
Threshold: $50 × 3 = $150
Transaction amount: $200
Result: +30 points (Amount exceeds threshold)
```

### Rule 2: Rapid-Fire Activity (+25 points)
**Triggers when:** 5 or more transactions within a 10-minute window

**Example:**
```
Transaction 1: 10:00 AM
Transaction 2: 10:02 AM
Transaction 3: 10:04 AM
Transaction 4: 10:06 AM
Transaction 5: 10:08 AM (triggers rule)
Result: +25 points for transaction 5
```

### Rule 3: Geographical Anomaly (+25 points)
**Triggers when:** Different location within 2 hours of previous transaction

**Example:**
```
Transaction 1: Location "New York", Time: 10:00 AM
Transaction 2: Location "Los Angeles", Time: 10:30 AM
Time difference: 30 minutes (< 2 hours)
Locations different: Yes
Result: +25 points
```

### Rule 4: Unusual Category (+20 points)
**Triggers when:** User has never used this category before

**Example:**
```
User's previous categories: Groceries, Utilities, Entertainment
New transaction category: Gambling
Result: +20 points (First time using "Gambling")
```

## Risk Levels

Based on the total fraud score:

- **LOW (0-39 points)**: Normal transaction
- **MEDIUM (40-69 points)**: Suspicious, review recommended
- **HIGH (70-100 points)**: Flagged as fraudulent, creates alert

## Testing Scenarios

### Scenario 1: High Amount Anomaly

**Setup:**
1. Create 3 normal transactions: $50, $60, $40
2. Average = $50

**Test:**
```
Create transaction:
- Amount: $200 (4× average)
- Type: EXPENSE
- Category: Shopping
- Description: Large purchase

Expected Result:
- Fraud Score: 30
- Risk Level: LOW
- Reason: "Amount $200.00 exceeds 3x user average $50.00"
```

### Scenario 2: Rapid-Fire Activity

**Test:**
```
Create 5 transactions within 10 minutes:

1. Amount: $10, Time: Now
2. Amount: $15, Time: Now + 2 min
3. Amount: $20, Time: Now + 4 min
4. Amount: $25, Time: Now + 6 min
5. Amount: $30, Time: Now + 8 min

Expected Result for Transaction 5:
- Fraud Score: 25
- Risk Level: LOW
- Reason: "5 or more transactions within 10 minutes"
```

**Note:** In the UI, you can't set exact timestamps, so create them quickly one after another.

### Scenario 3: Geographical Anomaly

**Test:**
```
Transaction 1:
- Amount: $50
- Location: New York
- Time: Now

Transaction 2 (within 2 hours):
- Amount: $60
- Location: Los Angeles
- Time: Now + 1 hour

Expected Result for Transaction 2:
- Fraud Score: 25
- Risk Level: LOW
- Reason: "Different location within 2 hours of previous transaction"
```

### Scenario 4: Unusual Category

**Setup:**
1. Create transactions with categories: Groceries, Utilities

**Test:**
```
Create transaction:
- Amount: $100
- Category: Gambling (never used before)
- Type: EXPENSE

Expected Result:
- Fraud Score: 20
- Risk Level: LOW
- Reason: "First time using category: Gambling"
```

### Scenario 5: Multiple Rules (HIGH Risk)

**Test to trigger HIGH risk (70+ points):**

```
Setup:
- User average: $50
- Previous categories: Groceries, Utilities
- Previous location: New York
- Previous transaction: 1 hour ago in New York

Create transaction:
- Amount: $200 (>3× average) → +30 points
- Category: Gambling (new) → +20 points
- Location: Los Angeles (different, <2 hours) → +25 points
- Total: 75 points

Expected Result:
- Fraud Score: 75
- Risk Level: HIGH
- Fraudulent: true
- Creates Fraud Alert
- Reasons: All 3 rules triggered
```

### Scenario 6: Multiple Rules (MEDIUM Risk)

**Test to trigger MEDIUM risk (40-69 points):**

```
Setup:
- User average: $50
- Previous categories: Groceries

Create transaction:
- Amount: $180 (>3× average) → +30 points
- Category: Entertainment (new) → +20 points
- Total: 50 points

Expected Result:
- Fraud Score: 50
- Risk Level: MEDIUM
- Fraudulent: false (< 70)
- No alert created
```

## Step-by-Step Testing

### Test 1: High Amount Only

1. Go to Transactions page
2. Create 3 transactions:
   - $50, Groceries
   - $60, Utilities
   - $40, Entertainment
3. Create test transaction:
   - Amount: $200
   - Type: EXPENSE
   - Category: Shopping
4. Check fraud score: Should be 30
5. Check risk badge: Should be LOW (green)

### Test 2: Rapid-Fire

1. Go to Transactions page
2. Quickly create 5 transactions (as fast as possible):
   - $10, Groceries
   - $15, Groceries
   - $20, Groceries
   - $25, Groceries
   - $30, Groceries
3. Check the 5th transaction
4. Fraud score should be 25
5. Risk badge: LOW

### Test 3: New Category

1. Create 2 transactions with category "Groceries"
2. Create transaction with category "Gambling"
3. Fraud score should be 20
4. Risk badge: LOW

### Test 4: Combination (HIGH)

1. Setup: Create 3 transactions averaging $50
2. Create transaction:
   - Amount: $200 (triggers Rule 1: +30)
   - Category: Gambling (triggers Rule 4: +20)
   - Location: Los Angeles (if previous was different location within 2 hours: +25)
3. Total score: 50-75 points
4. If 70+: Risk badge HIGH (red), Fraudulent flag set
5. Check Fraud Alerts page - should see new alert

## Checking Results

### In Transaction List

Each transaction shows:
- **Fraud Score**: Number 0-100
- **Risk Badge**: 
  - GREEN (LOW): 0-39
  - YELLOW (MEDIUM): 40-69
  - RED (HIGH): 70-100
- **Status**: COMPLETED or FLAGGED

### In Fraud Alerts Page

When fraud score ≥ 70:
- New alert appears
- Shows transaction details
- Shows risk level
- Shows reasons for detection
- Can be resolved by clicking "Resolve"

### In Backend Logs

```bash
docker logs -f finsight-backend | grep -i fraud
```

Look for:
```
High amount anomaly detected for transaction: amount=200, avg=50, threshold=150
Rapid-fire activity detected for user: 1
Geographical anomaly detected for transaction: location=Los Angeles
Unusual category detected: Gambling
HIGH FRAUD SCORE: 75 for transaction of user 1. Reasons: Amount $200.00 exceeds 3x user average $50.00; First time using category: Gambling; Different location within 2 hours of previous transaction
```

## Common Issues

### Issue: Fraud score is always 0

**Possible causes:**
1. No previous transactions to calculate average
2. Amount not high enough (must be >3× average)
3. Category already used before
4. No location data
5. Transactions not close enough in time

**Solution:** Follow test scenarios exactly

### Issue: Rapid-fire not triggering

**Cause:** Transactions not created fast enough or timestamps too far apart

**Solution:** Create transactions as quickly as possible, one after another

### Issue: Geographical anomaly not triggering

**Cause:** 
- No location in previous transaction
- More than 2 hours between transactions
- Same location

**Solution:** 
- Add location to both transactions
- Create second transaction within 2 hours
- Use different locations

### Issue: Unusual category not triggering

**Cause:** Category already used in a previous transaction

**Solution:** Use a completely new category that hasn't been used before

## Expected Behavior Summary

| Rule | Points | Condition | Easy to Test? |
|------|--------|-----------|---------------|
| High Amount | +30 | Amount > 3× avg | ✅ Yes |
| Rapid-Fire | +25 | 5+ in 10 min | ⚠️ Moderate |
| Geo Anomaly | +25 | Diff location < 2hr | ⚠️ Moderate |
| New Category | +20 | Never used before | ✅ Yes |

## Quick Test Commands

### Check if fraud detection is working:

```bash
# Watch fraud detection logs
docker logs -f finsight-backend 2>&1 | grep -i "fraud\|score"

# Check for high scores
docker logs finsight-backend 2>&1 | grep "HIGH FRAUD SCORE"

# Check for alerts created
docker logs finsight-backend 2>&1 | grep "Created fraud alert"
```

## Verification Checklist

After creating a test transaction, verify:

- [ ] Transaction appears in list
- [ ] Fraud score is displayed
- [ ] Risk badge shows correct color
- [ ] If score ≥ 70, status shows "FLAGGED"
- [ ] If score ≥ 70, alert appears in Fraud Alerts page
- [ ] Backend logs show fraud detection analysis
- [ ] Reasons are logged in backend

## Tips for Successful Testing

1. **Start fresh**: Clear transactions or use a new user
2. **Build up gradually**: Create normal transactions first
3. **Test one rule at a time**: Easier to verify
4. **Check logs**: Backend logs show detailed analysis
5. **Use extreme values**: Make it obvious (e.g., $1000 when average is $50)
6. **Be patient**: Wait for page to refresh after creating transaction

## Summary

The fraud detection system is fully functional and follows all requirements:

✅ Rule 1: High Amount (>3× avg) → +30 points
✅ Rule 2: Rapid-Fire (5+ in 10 min) → +25 points  
✅ Rule 3: Geo Anomaly (diff location < 2hr) → +25 points
✅ Rule 4: New Category → +20 points
✅ Risk Levels: LOW (0-39), MEDIUM (40-69), HIGH (70-100)
✅ Fraud Alerts: Created when score ≥ 70
✅ Detailed logging for debugging

Follow the test scenarios above to verify each rule! 🚀
