# Subscription Validation

## Overview
Added comprehensive validation for manually adding subscriptions, including date range validation (25-35 days), future date checks, and amount limits.

## Backend Changes

### SubscriptionDto.java

Added validation annotations:

```java
@NotBlank(message = "Subscription name is required")
@Size(max = 100, message = "Subscription name must not exceed 100 characters")
private String merchant;

@NotNull(message = "Amount is required")
@DecimalMin(value = "0.01", message = "Amount must be at least $0.01")
@DecimalMax(value = "10000.00", message = "Amount cannot exceed $10,000")
private BigDecimal avgAmount;

@NotNull(message = "Last paid date is required")
@PastOrPresent(message = "Last paid date cannot be in the future")
private LocalDate lastPaidDate;

@NotNull(message = "Next due date is required")
@Future(message = "Next due date must be in the future")
private LocalDate nextDueDate;
```

### SubscriptionController.java

Added custom validation for date range:

```java
@PostMapping
public ResponseEntity<SubscriptionDto> createSubscription(@Valid @RequestBody SubscriptionDto dto, ...) {
    // Validate date range (25-35 days)
    long daysBetween = ChronoUnit.DAYS.between(dto.getLastPaidDate(), dto.getNextDueDate());
    
    if (daysBetween < 25) {
        throw new RuntimeException("Next due date must be at least 25 days after last paid date");
    }
    if (daysBetween > 35) {
        throw new RuntimeException("Next due date must be at most 35 days after last paid date");
    }
    // ...
}
```

## Frontend Changes (Subscriptions.tsx)

### 1. Added Error State

```typescript
const [formErrors, setFormErrors] = useState<Record<string, string>>({});
```

### 2. Client-Side Validation

Comprehensive validation before submission:

```typescript
// Merchant validation
if (!addForm.merchant.trim()) {
  errors.merchant = 'Subscription name is required';
} else if (addForm.merchant.length > 100) {
  errors.merchant = 'Subscription name must not exceed 100 characters';
}

// Amount validation
if (isNaN(amount) || amount <= 0) {
  errors.avgAmount = 'Amount must be greater than $0';
} else if (amount > 10000) {
  errors.avgAmount = 'Amount cannot exceed $10,000';
}

// Last paid date validation
if (lastPaid > now) {
  errors.lastPaidDate = 'Last paid date cannot be in the future';
}

// Next due date validation
if (nextDue <= now) {
  errors.nextDueDate = 'Next due date must be in the future';
}

// Date range validation (25-35 days)
const daysBetween = Math.floor((nextDue.getTime() - lastPaid.getTime()) / (1000 * 60 * 60 * 24));

if (daysBetween < 25) {
  errors.nextDueDate = 'Next due date must be at least 25 days after last paid date';
} else if (daysBetween > 35) {
  errors.nextDueDate = 'Next due date must be at most 35 days after last paid date';
}
```

### 3. Visual Indicators

- **Red borders** on invalid fields
- **Error messages** below each field
- **HTML5 constraints:**
  - `max` on last paid date (today)
  - `min` on next due date (tomorrow)
  - `max` on amount (10000)
- **Helper text** explaining date range requirement
- **Errors clear** when user types

### 4. Form Updates

Each field now:
- Shows validation errors
- Has appropriate HTML5 constraints
- Clears errors on change
- Displays red border when invalid

## Validation Rules

### Subscription Name
- **Required**
- **Max length:** 100 characters

### Amount
- **Required**
- **Minimum:** $0.01
- **Maximum:** $10,000

### Last Paid Date
- **Required**
- **Cannot be in the future**
- HTML5 max: today

### Next Due Date
- **Required**
- **Must be in the future**
- **Must be 25-35 days after last paid date**
- HTML5 min: tomorrow

### Date Range
- **Minimum gap:** 25 days
- **Maximum gap:** 35 days
- Ensures subscriptions are monthly (approximately 30 days)

## Error Messages

### Subscription Name
- "Subscription name is required"
- "Subscription name must not exceed 100 characters"

### Amount
- "Amount must be greater than $0"
- "Amount cannot exceed $10,000"

### Last Paid Date
- "Last paid date is required"
- "Last paid date cannot be in the future"

### Next Due Date
- "Next due date is required"
- "Next due date must be in the future"
- "Next due date must be at least 25 days after last paid date"
- "Next due date must be at most 35 days after last paid date"

## Testing

### Test Case 1: Future Last Paid Date
1. Set last paid date to tomorrow
2. See error: "Last paid date cannot be in the future"
3. Field has red border
4. Change to today
5. Error clears

### Test Case 2: Past Next Due Date
1. Set next due date to today
2. See error: "Next due date must be in the future"
3. Field has red border
4. Change to future date
5. Error clears

### Test Case 3: Date Range Too Short
1. Last paid: Jan 1
2. Next due: Jan 20 (19 days)
3. See error: "Next due date must be at least 25 days after last paid date"
4. Change to Jan 26 (25 days)
5. Error clears

### Test Case 4: Date Range Too Long
1. Last paid: Jan 1
2. Next due: Feb 10 (40 days)
3. See error: "Next due date must be at most 35 days after last paid date"
4. Change to Feb 5 (35 days)
5. Error clears

### Test Case 5: Excessive Amount
1. Enter amount: $15,000
2. See error: "Amount cannot exceed $10,000"
3. Field has red border
4. Change to $50
5. Error clears

### Test Case 6: Valid Subscription
1. Name: "Netflix"
2. Amount: $15.99
3. Last paid: Jan 1
4. Next due: Jan 31 (30 days)
5. No errors
6. Successfully creates subscription

## Why 25-35 Days?

Monthly subscriptions typically charge every 28-31 days:
- **25 days minimum:** Prevents weekly/biweekly subscriptions
- **35 days maximum:** Ensures it's monthly, not bimonthly
- **Sweet spot:** 30 days (typical monthly subscription)

This range accommodates:
- February (28/29 days)
- 30-day months
- 31-day months
- Slight variations in billing cycles

## Files Modified

1. `backend/src/main/java/com/finsight/dto/SubscriptionDto.java` - Added validation annotations
2. `backend/src/main/java/com/finsight/controller/SubscriptionController.java` - Added date range validation
3. `frontend/src/pages/Subscriptions.tsx` - Added comprehensive client-side validation

## Benefits

1. **Prevents invalid data:** Can't add subscriptions with impossible dates
2. **Clear guidance:** Users know exactly what's required
3. **Immediate feedback:** Client-side validation is instant
4. **Consistent data:** All subscriptions follow the same pattern
5. **Better UX:** Red borders and specific error messages
6. **Prevents confusion:** Date range ensures monthly subscriptions only
