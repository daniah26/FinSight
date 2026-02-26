# Subscription Category Update

## Changes Made

### 1. Use Category as Subscription Name
Changed the subscription detection to use the transaction **category** field instead of the description field as the merchant/subscription name.

**Why:** Categories are more consistent and meaningful for grouping recurring payments (e.g., "Netflix", "Spotify", "Gym Membership") compared to transaction descriptions which might vary.

### 2. Auto-Detect on Page Load
The Subscriptions page now automatically detects/refreshes subscriptions every time you visit it, eliminating the need to click the "Refresh" button.

**Why:** Provides a better user experience - subscriptions are always up-to-date when you navigate to the page.

## Files Changed

### Backend
- `backend/src/main/java/com/finsight/service/SubscriptionDetectorService.java`
  - Changed `groupByMerchant()` to group by `category` instead of `description`
  - Changed `createSubscription()` to use `category` as the merchant name
  - Simplified `normalizeMerchant()` to just lowercase the category name

### Frontend
- `frontend/src/pages/Subscriptions.js`
  - Changed `loadSubscriptions()` to always call `detectSubscriptions()` instead of checking for existing subscriptions first
  - This ensures subscriptions are refreshed every time the page loads

## How It Works Now

1. **Create transactions with categories:**
   - Transaction 1: Category = "Netflix", Amount = $15.99, Date = Jan 1
   - Transaction 2: Category = "Netflix", Amount = $15.99, Date = Feb 1
   
2. **Visit Subscriptions page:**
   - Page automatically detects the recurring pattern
   - Creates a subscription named "Netflix" (from the category)
   - No need to click "Refresh"

3. **Add more transactions:**
   - Transaction 3: Category = "Spotify", Amount = $10.99, Date = Jan 5
   - Transaction 4: Category = "Spotify", Amount = $10.99, Date = Feb 5

4. **Visit Subscriptions page again:**
   - Page automatically refreshes
   - Now shows both "Netflix" and "Spotify" subscriptions

## Testing

1. Go to Transactions page
2. Create a transaction with category "Test Sub A", type EXPENSE, amount $10
3. Create another transaction with category "Test Sub A", date 30 days later, amount $10
4. Navigate to Subscriptions page
5. Verify: "Test Sub A" subscription appears automatically without clicking refresh
6. Create transactions for "Test Sub B" (2 transactions, 30 days apart)
7. Navigate away and back to Subscriptions page
8. Verify: Both "Test Sub A" and "Test Sub B" appear

## Notes

- Subscriptions are grouped by category name (case-insensitive)
- Each unique category with recurring payments becomes a separate subscription
- The page auto-refreshes on every visit, so subscriptions are always current
- You still need to restart the backend for the changes to take effect
