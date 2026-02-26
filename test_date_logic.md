# Date Logic Test

Current date: February 26, 2026

## Expected behavior:
```
monthIndex=0, monthsBack=3: Feb 2026 - 3 months = November 2025
monthIndex=1, monthsBack=2: Feb 2026 - 2 months = December 2025  
monthIndex=2, monthsBack=1: Feb 2026 - 1 month = January 2026
monthIndex=3, monthsBack=0: Feb 2026 - 0 months = February 2026
```

## To debug:

1. Check backend logs after reseeding - look for lines like:
   ```
   Month 1: NOVEMBER 2025 - Generating 10 transactions
   Month 2: DECEMBER 2025 - Generating 12 transactions
   Month 3: JANUARY 2026 - Generating 14 transactions
   Month 4: FEBRUARY 2026 - Generating 15 transactions
   ```

2. If logs show all 4 months but frontend only shows 2, the issue is in the frontend or database query

3. If logs only show 2 months, there's a logic error in the code

## Quick database check:

Run this SQL query directly on your database:
```sql
SELECT 
    DATE_FORMAT(transaction_date, '%Y-%m') as month,
    COUNT(*) as count
FROM transactions 
WHERE user_id = 1
GROUP BY DATE_FORMAT(transaction_date, '%Y-%m')
ORDER BY month;
```

This will show you exactly which months have transactions in the database.
