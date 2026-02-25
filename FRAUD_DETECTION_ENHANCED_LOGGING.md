# 🔍 Enhanced Fraud Detection Logging

## What Was Added

Added comprehensive logging to fraud detection to help debug why rules aren't triggering. Now you can see exactly what's happening with each rule.

## New Logging Output

When a transaction is created, you'll see detailed logs like this:

```
=== Starting fraud detection for transaction 123 ===
Amount: 200.00, Category: Gambling, Location: Los Angeles, Time: 2024-02-25T10:00:00

Rule 1 - High Amount: User average = 50.00
Rule 1 - Threshold (3x avg) = 150.00, Transaction amount = 200.00
Rule 1 TRIGGERED: High amount anomaly (+30 points)

Rule 2 - Checking rapid-fire activity...
Rapid-fire check: 2 transactions in 10-minute window (need 5+)
Rule 2 NOT triggered: Less than 5 transactions in 10-minute window

Rule 3 - Checking geographical anomaly, location: Los Angeles
Geo check: Previous location = New York, Current location = Los Angeles, Hours between = 1
Geo check: Different location = true, Within 2 hours = true
Rule 3 TRIGGERED: Geographical anomaly (+25 points)

Rule 4 - Checking unusual category: Gambling
Category check: User has used 2 categories: [Groceries, Utilities]
Category check: Category 'Gambling' used 1 times
Rule 4 TRIGGERED: Unusual category (+20 points)

=== Fraud detection complete: Score = 75.0, Risk = HIGH, Fraudulent = true ===
HIGH FRAUD SCORE: 75.0 for transaction 123. Reasons: Amount $200.00 exceeds 3x user average $50.00; Different location within 2 hours of previous transaction; First time using category: Gambling
```

## How to Use

### Watch Logs in Real-Time

```bash
docker logs -f finsight-backend | grep -E "fraud|Rule|TRIGGERED|NOT triggered"
```

### Check Why a Rule Didn't Trigger

```bash
# Check all fraud detection logs
docker logs finsight-backend 2>&1 | grep "=== Starting fraud"

# Check specific rule
docker logs finsight-backend 2>&1 | grep "Rule 1"
docker logs finsight-backend 2>&1 | grep "Rule 2"
docker logs finsight-backend 2>&1 | grep "Rule 3"
docker logs finsight-backend 2>&1 | grep "Rule 4"
```

## What Each Rule Logs

### Rule 1: High Amount
```
Rule 1 - High Amount: User average = 50.00
Rule 1 - Threshold (3x avg) = 150.00, Transaction amount = 200.00
Rule 1 TRIGGERED: High amount anomaly (+30 points)
```
OR
```
Rule 1 NOT triggered: Amount within normal range
```
OR
```
Rule 1 SKIPPED: No previous transactions to calculate average
```

### Rule 2: Rapid-Fire
```
Rule 2 - Checking rapid-fire activity...
Rapid-fire check: 5 transactions in 10-minute window (need 5+)
Rule 2 TRIGGERED: Rapid-fire activity (+25 points)
```
OR
```
Rule 2 NOT triggered: Less than 5 transactions in 10-minute window
```

### Rule 3: Geographical Anomaly
```
Rule 3 - Checking geographical anomaly, location: Los Angeles
Geo check: Previous location = New York, Current location = Los Angeles, Hours between = 1
Geo check: Different location = true, Within 2 hours = true
Rule 3 TRIGGERED: Geographical anomaly (+25 points)
```
OR
```
Rule 3 NOT triggered: No geographical anomaly detected
```
OR
```
Rule 3 SKIPPED: No location provided
```

### Rule 4: Unusual Category
```
Rule 4 - Checking unusual category: Gambling
Category check: User has used 2 categories: [Groceries, Utilities]
Category check: Category 'Gambling' used 1 times
Rule 4 TRIGGERED: Unusual category (+20 points)
```
OR
```
Rule 4 NOT triggered: Category has been used before
```

## Debugging Common Issues

### Issue: Only Rule 1 (High Amount) Triggers

**Check logs for:**
```bash
docker logs finsight-backend 2>&1 | grep "Rule 2\|Rule 3\|Rule 4"
```

**Possible reasons:**
- Rule 2: Not creating 5 transactions within 10 minutes
- Rule 3: No location provided, or same location, or >2 hours apart
- Rule 4: Category already used before

### Issue: Rule 2 (Rapid-Fire) Never Triggers

**Check logs:**
```
Rapid-fire check: X transactions in 10-minute window (need 5+)
```

**Solution:** Create 5 transactions very quickly (within 10 minutes)

### Issue: Rule 3 (Geo Anomaly) Never Triggers

**Check logs:**
```
Rule 3 SKIPPED: No location provided
```
OR
```
Geo check: No previous transaction with location found
```

**Solution:** 
- Add location to transactions
- Make sure previous transaction also has location
- Create second transaction within 2 hours
- Use different locations

### Issue: Rule 4 (Unusual Category) Never Triggers

**Check logs:**
```
Category check: Category 'X' used Y times
```

**Solution:** Use a completely new category that hasn't been used before

## Rebuild and Test

```bash
docker-compose down
docker-compose up --build backend
```

Then create a transaction and watch the logs:

```bash
docker logs -f finsight-backend | grep -E "fraud|Rule"
```

## Summary

With enhanced logging, you can now:

✅ See exactly which rules are being checked
✅ See why rules trigger or don't trigger
✅ Debug fraud detection issues easily
✅ Understand the fraud score calculation
✅ Verify all rules are working correctly

The logs will tell you exactly what's happening! 🔍
