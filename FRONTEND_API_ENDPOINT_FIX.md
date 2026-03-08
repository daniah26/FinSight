# Frontend API Endpoint Fix

## Problem
The frontend was calling `/api/auth/register` but the backend endpoint is `/api/auth/signup`, causing a 500 error.

## Root Cause
When the frontend was converted to TypeScript, the API endpoint for registration was incorrectly set to `/auth/register` instead of `/auth/signup`.

## Fix Applied

### Frontend (api.ts)
Changed the registration endpoint to match the backend:

**Before:**
```typescript
export const registerUser = (data: { username: string; email: string; password: string }) =>
  api.post('/auth/register', data);
```

**After:**
```typescript
export const registerUser = (data: { username: string; email: string; password: string }) =>
  api.post('/auth/signup', data);
```

## Backend Endpoints (No Changes Needed)

The backend has these auth endpoints:
- `POST /api/auth/signup` - Register new user
- `POST /api/auth/login` - Login existing user

## Files Modified

1. `frontend/src/lib/api.ts` - Fixed registration endpoint

## Testing

After this fix, registration should work:

1. Go to the signup page
2. Enter username, email, and password
3. Click "Sign Up"
4. Should successfully create account and redirect to dashboard

## Summary

This was a simple endpoint mismatch - the frontend was calling the wrong URL. No backend changes were needed since the backend was working correctly all along.
