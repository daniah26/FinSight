# Subscription Page Fixes

## Issues Fixed

### 1. Ignore Button Not Removing Subscriptions
**Problem**: Clicking "Ignore" would mark the subscription as IGNORED but it would still appear in the list (just grayed out).

**Solution**:
- Added confirmation modal popup before ignoring a subscription
- Filtered out IGNORED subscriptions from the display
- Updated subscription count to exclude ignored subscriptions
- Modal includes merchant name and clear warning about hiding the subscription

**Changes**:
- `frontend/src/pages/Subscriptions.js`: Added `confirmIgnore` state, `handleIgnoreClick`, `handleConfirmIgnore`, and `handleCancelIgnore` functions
- `frontend/src/pages/Subscriptions.css`: Added modal overlay and content styles
- `frontend/src/components/Button.css`: Danger variant already existed
- Filtered subscriptions display to exclude IGNORED status

### 2. Alert System Not Working Correctly
**Problem**: The alert system wasn't showing subscriptions due within 7 days consistently because the main `getSubscriptions()` endpoint was calling `detectSubscriptions()` which deleted and recreated all subscriptions, losing their IGNORED status.

**Solution**:
- Changed `GET /api/subscriptions` to fetch existing subscriptions from database instead of re-detecting
- Created new `POST /api/subscriptions/detect` endpoint for manual subscription detection/refresh
- Modified `detectSubscriptions()` to merge with existing subscriptions instead of deleting them
- Preserved subscription status (ACTIVE/IGNORED) during updates
- The due-soon alert now consistently shows subscriptions due within 7 days

**Changes**:
- `backend/src/main/java/com/finsight/controller/SubscriptionController.java`: Split GET endpoint to just fetch, added POST /detect endpoint
- `backend/src/main/java/com/finsight/repository/SubscriptionRepository.java`: Added `findByUserId()` method
- `backend/src/main/java/com/finsight/service/SubscriptionDetectorService.java`: 
  - Removed `subscriptionRepository.deleteAll(existingSubs)`
  - Added logic to merge with existing subscriptions
  - Added `updateSubscription()` method to update existing subscriptions while preserving status

### 3. Second Subscription Not Appearing
**Problem**: When adding a second subscription through manual transactions, it wouldn't appear because `detectSubscriptions()` was deleting all subscriptions and recreating them, potentially missing new patterns.

**Solution**:
- Same fix as issue #2 - the merge logic now properly detects new subscriptions while preserving existing ones
- New subscriptions are created with ACTIVE status
- Existing subscriptions are updated with new transaction data but keep their status
- The detection algorithm now properly handles multiple subscriptions per user

**Changes**: Same as issue #2

## How It Works Now

1. **Initial Load**: Frontend calls `GET /api/subscriptions` which fetches existing subscriptions from database
2. **Due Soon Alert**: Always shows if there are ACTIVE subscriptions due within 7 days
3. **Ignore Action**: 
   - User clicks "Ignore" → Confirmation modal appears
   - User confirms → Subscription status set to IGNORED
   - Subscription is filtered out from display and alerts
4. **New Transactions**: When new transactions are added, the detection service:
   - Analyzes all transactions for recurring patterns
   - Merges with existing subscriptions (updates amounts/dates, preserves status)
   - Creates new subscriptions for newly detected patterns
   - Never deletes existing subscriptions

## Testing

To test these fixes:

1. **Test Ignore with Confirmation**:
   - Go to Subscriptions page
   - Click "Ignore" on any subscription
   - Verify modal appears with merchant name
   - Click "Cancel" - modal closes, subscription remains
   - Click "Ignore" again and confirm - subscription disappears from list

2. **Test Alert System**:
   - Ensure you have a subscription with next due date within 7 days
   - Refresh the page
   - Verify the yellow "Upcoming Payments" banner appears at the top
   - Verify the subscription card has "Due Soon" badge

3. **Test Multiple Subscriptions**:
   - Add manual transactions for a new recurring merchant (2+ transactions, 20-40 days apart)
   - Go to Subscriptions page
   - Verify both old and new subscriptions appear
   - Ignore one subscription
   - Add more transactions
   - Verify ignored subscription stays hidden, new ones appear
