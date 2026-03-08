# Subscription Frontend Fixes

## Issues Found

1. **Not auto-detecting on page load** - The new frontend only called `getSubscriptions()`, not `detectSubscriptions()`
2. **Date parsing issues** - The date parsing didn't properly handle edge cases
3. **No error handling** - Missing proper error handling and logging
4. **Add subscription modal not functional** - The modal has no state or submit handler (still needs implementation)

## Fixes Applied

### 1. Auto-Detection on Load (Subscriptions.tsx)

**Before:**
```typescript
useEffect(() => {
  const fetchSubscriptions = async () => {
    const res = await getSubscriptions(user.id);
    setSubscriptions(res.data);
  };
  fetchSubscriptions();
}, [user?.id]);
```

**After:**
```typescript
useEffect(() => {
  const fetchSubscriptions = async () => {
    // Auto-detect subscriptions on load
    await detectSubscriptions(user.id);
    const res = await getSubscriptions(user.id);
    const data = res.data?.content || res.data || [];
    setSubscriptions(Array.isArray(data) ? data : []);
  };
  fetchSubscriptions();
}, [user?.id]);
```

### 2. Improved Date Parsing

**Before:**
```typescript
const formatDate = (dateStr: string) => {
  const [y, m, d] = dateStr.split('T')[0].split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString(...);
};
```

**After:**
```typescript
const formatDate = (dateStr: string) => {
  // Handle both ISO datetime (2024-01-15T00:00:00) and LocalDate (2024-01-15)
  const dateOnly = dateStr.split('T')[0];
  const [y, m, d] = dateOnly.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString(...);
};

const getDaysUntil = (dateStr: string) => {
  const dateOnly = dateStr.split('T')[0];
  const [y, m, d] = dateOnly.split('-').map(Number);
  const due = new Date(y, m - 1, d);
  due.setHours(0, 0, 0, 0); // Ensure time is normalized
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  return Math.ceil((due.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
};
```

### 3. Added Error Handling and Logging

Added console logging to help debug issues:
- Log when detection starts
- Log detection response
- Log get subscriptions response
- Log processed data
- Log errors with full details

### 4. Improved Detect Button Handler

Added error clearing and better error logging:
```typescript
const handleDetect = async () => {
  try {
    setDetecting(true);
    setError('');
    await detectSubscriptions(user.id);
    const res = await getSubscriptions(user.id);
    const data = res.data?.content || res.data || [];
    setSubscriptions(Array.isArray(data) ? data : []);
  } catch (err: any) {
    console.error('Detection error:', err);
    setError(err.response?.data?.message || 'Failed to detect subscriptions');
  } finally {
    setDetecting(false);
  }
};
```

## How It Works Now

1. **On page load:**
   - Automatically calls `detectSubscriptions(userId)` to analyze transactions
   - Then calls `getSubscriptions(userId)` to get the detected subscriptions
   - Displays all active subscriptions

2. **When clicking "Detect" button:**
   - Manually triggers detection
   - Refreshes the subscription list
   - Shows loading state

3. **Date handling:**
   - Properly parses both ISO datetime and LocalDate formats
   - Normalizes times to midnight for accurate day calculations
   - Correctly identifies subscriptions due within 7 days

## Still TODO

The "Add Subscription" modal is not functional yet. It needs:
- State management for form fields
- Submit handler to create a subscription
- Backend endpoint to manually add subscriptions (if not already exists)

## Testing

1. Create transactions with the same category 30 days apart
2. Navigate to Subscriptions page
3. Should automatically detect and show the subscription
4. Dates should be displayed correctly
5. "Due Soon" badge should appear for subscriptions due within 7 days
6. Click "Detect" button should refresh the list

## Files Modified

- `frontend/src/pages/Subscriptions.tsx` - Fixed auto-detection, date parsing, and error handling
