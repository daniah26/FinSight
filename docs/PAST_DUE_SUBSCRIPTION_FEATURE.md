# Past Due Subscription Feature

## Overview
Added visual indicators for past due subscriptions and verified that subscription dates update automatically when payments are made.

## Frontend Changes (Subscriptions.tsx)

### 1. Added Past Due Detection

Added a new `useMemo` hook to identify overdue subscriptions:

```typescript
const pastDue = useMemo(() => activeSubs.filter(s => {
  const days = getDaysUntil(s.nextDueDate);
  return days < 0; // Negative days means past due
}), [activeSubs]);
```

### 2. Added Past Due Banner

Similar to the "Due Soon" banner, added a red banner for overdue subscriptions:

```typescript
{pastDue.length > 0 && (
  <div className="bg-destructive/10 border border-destructive/25">
    <p>Past Due Payments</p>
    <p>You have {pastDue.length} overdue subscription(s)</p>
  </div>
)}
```

### 3. Updated Subscription Cards

Enhanced the card styling to show three states:

1. **Past Due** (Red)
   - Border: `border-destructive/30`
   - Background: `bg-destructive/5`
   - Badge: Red "Past Due" badge
   - Shows days overdue: "15d overdue"

2. **Due Soon** (Yellow/Warning)
   - Border: `border-warning/30`
   - Background: `bg-warning/5`
   - Badge: Yellow "Due Soon" badge
   - Shows days until due: "3d"

3. **Active** (Green)
   - Normal border and background
   - Badge: Green "Active" badge
   - No special indicators

### Visual Hierarchy

```typescript
const isPastDue = daysUntil < 0;
const isDueSoon = daysUntil >= 0 && daysUntil <= 7;

// Card styling
className={`
  ${isPastDue ? 'border-destructive/30 bg-destructive/5' : 
    isDueSoon ? 'border-warning/30 bg-warning/5' : 
    'border-border bg-card'}
`}

// Badge
{isPastDue ? <Badge>Past Due</Badge> :
 isDueSoon ? <Badge>Due Soon</Badge> :
 <Badge>Active</Badge>}

// Date display
{isPastDue && ` · ${Math.abs(daysUntil)}d overdue`}
{isDueSoon && ` · ${daysUntil}d`}
```

## Backend Behavior (Already Working)

The backend `SubscriptionDetectorService` already handles date updates correctly:

### When a Transaction is Added

1. User adds a transaction with category "Netflix"
2. User visits Subscriptions page (or clicks Refresh)
3. Frontend calls `detectSubscriptions(userId)`
4. Backend:
   - Finds all transactions for "Netflix" category
   - Sorts by date (ascending)
   - Takes the most recent transaction
   - Updates `lastPaidDate` to that transaction's date
   - Calculates `nextDueDate` as `lastPaidDate + 30 days`
   - Saves the updated subscription

### Example Flow

**Initial State:**
- Subscription: Netflix
- Last Paid: Jan 15, 2024
- Next Due: Feb 15, 2024
- Status: Past Due (today is Mar 1, 2024)

**User adds transaction:**
- Category: Netflix
- Amount: $15.99
- Date: Mar 1, 2024

**After detection:**
- Subscription: Netflix
- Last Paid: Mar 1, 2024 ✓ (updated)
- Next Due: Mar 31, 2024 ✓ (updated)
- Status: Active ✓ (no longer past due)

## Features

1. **Visual Indicators:**
   - Red banner for past due subscriptions
   - Red card border and background
   - "Past Due" badge
   - Shows days overdue

2. **Automatic Updates:**
   - Dates update when you add transactions
   - Auto-detects on page load
   - Manual refresh button available

3. **Priority Display:**
   - Past due subscriptions shown first (red)
   - Due soon subscriptions shown next (yellow)
   - Active subscriptions shown last (green)

## Testing

1. **Create a past due subscription:**
   - Add subscription manually
   - Set Next Due Date to a past date (e.g., 10 days ago)
   - Verify red "Past Due" badge appears
   - Verify banner shows "Past Due Payments"
   - Verify card shows "10d overdue"

2. **Pay the subscription:**
   - Go to Transactions page
   - Add transaction with same category
   - Set date to today
   - Go back to Subscriptions page
   - Verify subscription is now "Active"
   - Verify dates are updated
   - Verify "Past Due" badge is gone

3. **Test due soon:**
   - Set Next Due Date to 3 days from now
   - Verify yellow "Due Soon" badge
   - Verify shows "3d"

## Files Modified

- `frontend/src/pages/Subscriptions.tsx` - Added past due detection and visual indicators

## Notes

- Past due detection is purely visual - no backend changes needed
- Backend already handles date updates correctly through `detectSubscriptions`
- The page auto-refreshes on load, so dates update automatically
- Manual refresh button also triggers detection
- Past due subscriptions are sorted by priority (most urgent first)
