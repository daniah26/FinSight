# Frontend-Backend API Compatibility Fixes

## Issues Found and Fixed

### 1. Authentication Response Mismatch

**Problem:** 
- Backend returns `userId` but frontend expected `id`
- Backend doesn't return `role` but frontend expected it

**Backend Response (AuthResponse.java):**
```java
{
  userId: Long,
  username: String,
  email: String,
  token: String,
  message: String
}
```

**Frontend Expected:**
```typescript
{
  id: number,
  username: string,
  role: string,
  token: string
}
```

**Fix Applied:**
- Updated `Login.tsx` to use `response.data.userId` instead of `response.data.id`
- Updated `Signup.tsx` to use `response.data.userId` instead of `response.data.id`
- Set default role to `'USER'` since backend doesn't provide it

### 2. Dashboard Summary Structure Mismatch

**Problem:**
- Frontend types didn't match the actual backend DashboardSummary structure

**Backend Response (DashboardSummary.java):**
```java
{
  totalIncome: BigDecimal,
  totalExpenses: BigDecimal,
  currentBalance: BigDecimal,
  totalFlaggedTransactions: Long,
  averageFraudScore: Double,
  spendingByCategory: Map<String, BigDecimal>,
  fraudByCategory: Map<String, Long>,
  spendingTrends: List<TimeSeriesPoint>
}
```

**TimeSeriesPoint:**
```java
{
  date: LocalDate,
  amount: BigDecimal
}
```

**Fix Applied:**
- Updated `types.ts` to match backend structure
- Changed `categoryBreakdown` and `timeSeriesPoints` to `spendingByCategory` and `spendingTrends`
- Updated TimeSeriesPoint to have `date` and `amount` fields only

### 3. Transaction Response Structure

**Problem:**
- Frontend expected `userId` field that backend doesn't return
- Backend has additional fields (`status`, `fraudReasons`) that frontend didn't know about

**Fix Applied:**
- Removed `userId` from TransactionResponse type
- Added optional `status` and `fraudReasons` fields

### 4. Subscription DTO Structure

**Problem:**
- Frontend expected `userId` field that backend doesn't return
- Backend has `createdAt` field that frontend didn't know about

**Fix Applied:**
- Removed `userId` from SubscriptionDto type
- Added `createdAt` field

## Files Modified

1. `frontend/src/pages/Login.tsx` - Fixed auth response handling
2. `frontend/src/pages/Signup.tsx` - Fixed auth response handling
3. `frontend/src/types.ts` - Updated all type definitions to match backend DTOs

## Testing Checklist

- [ ] Login with existing user works
- [ ] Signup with new user works
- [ ] Dashboard loads without errors
- [ ] Transactions page displays correctly
- [ ] Fraud Alerts page works
- [ ] Subscriptions page works
- [ ] No console errors related to API responses

## Notes

- The frontend now correctly maps backend field names
- All TypeScript types now match the actual Java DTOs
- The `mockData.ts` file already had correct types and didn't need changes
- Default role is set to 'USER' since the backend doesn't provide it (consider adding role to backend if needed)

## Potential Backend Improvements

Consider these backend changes for better consistency:

1. **Add role field to AuthResponse:**
   ```java
   private String role; // USER, ADMIN, etc.
   ```

2. **Return id instead of userId for consistency:**
   ```java
   private Long id; // instead of userId
   ```

3. **Add userId to TransactionResponse if needed by frontend:**
   ```java
   private Long userId;
   ```
