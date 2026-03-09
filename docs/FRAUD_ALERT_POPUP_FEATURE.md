# Fraud Alert Popup Feature

## Overview
Added a popup modal that appears when a user creates a transaction that triggers fraud detection. The modal displays fraud details and includes a "View All Alerts" button that navigates to the Fraud Alerts page.

## Features

### Fraud Alert Modal
- **Automatic Detection**: Shows immediately after creating a fraudulent transaction
- **Fraud Score Display**: Large, prominent fraud score (0-100)
- **Risk Level Badge**: Visual indicator (LOW, MEDIUM, HIGH, CRITICAL)
- **Transaction Details**: Amount, category, location, description
- **Fraud Reasons**: Bullet list explaining why the transaction was flagged
- **Action Buttons**:
  - "Dismiss": Closes the modal
  - "View All Alerts": Navigates to /fraud-alerts page

### Visual Design
- **Gradient Background**: Red-to-yellow gradient for fraud score section
- **Risk Icons**: Emoji indicators (⚠️ for LOW/MEDIUM, 🚨 for HIGH, 🔴 for CRITICAL)
- **Color-Coded Sections**: Different backgrounds for different information types
- **Responsive**: Works on mobile and desktop
- **Smooth Animations**: Fade-in and slide-up effects

## Files Created

### 1. FraudAlertModal.js
Location: `frontend/src/components/FraudAlertModal.js`

Component that displays the fraud alert popup with:
- Modal overlay with click-to-close
- Fraud score visualization
- Transaction summary
- Fraud reasons list
- Navigation to fraud alerts page

### 2. FraudAlertModal.css
Location: `frontend/src/components/FraudAlertModal.css`

Styling for the modal including:
- Overlay and modal animations
- Responsive design
- Color-coded sections
- Badge styling

## Files Modified

### 1. TransactionResponse.java
**Location**: `backend/src/main/java/com/finsight/dto/TransactionResponse.java`

**Changes**:
- Added `List<String> fraudReasons` field
- Imported `java.util.List`

**Purpose**: Include fraud detection reasons in API response

### 2. TransactionService.java
**Location**: `backend/src/main/java/com/finsight/service/TransactionService.java`

**Changes**:
- Modified `createTransaction()` to pass `FraudDetectionResult` to `toResponse()`
- Updated `toResponse()` signature to accept `FraudDetectionResult` instead of `RiskLevel`
- Added `fraudReasons` to response builder
- Updated `findWithFilters()` to pass `null` for fraud result (historical data)

**Purpose**: Return fraud reasons when creating transactions

### 3. Transactions.js
**Location**: `frontend/src/pages/Transactions.js`

**Changes**:
- Imported `FraudAlertModal` component
- Added state: `showFraudModal`, `fraudData`
- Modified `handleSubmit()` to:
  - Check response for fraud detection
  - Extract fraud data
  - Show modal if fraud detected
- Added `<FraudAlertModal>` component to JSX

**Purpose**: Display fraud alert popup when transaction is flagged

## How It Works

### Flow Diagram
```
User Creates Transaction
        ↓
Backend Fraud Detection
        ↓
Transaction Saved with Fraud Score
        ↓
Response Includes:
  - fraudulent: true/false
  - fraudScore: 0-100
  - riskLevel: LOW/MEDIUM/HIGH/CRITICAL
  - fraudReasons: ["reason 1", "reason 2"]
        ↓
Frontend Checks Response
        ↓
If fraudScore > 0:
  - Extract fraud data
  - Show FraudAlertModal
        ↓
User Actions:
  - Click "Dismiss" → Close modal
  - Click "View All Alerts" → Navigate to /fraud-alerts
```

## Testing

### Test Case 1: High Amount Transaction
1. Go to Transactions page
2. Click "+ Add Transaction"
3. Enter:
   - Amount: $10,000
   - Category: luxury_electronics
   - Location: New York
4. Submit
5. **Expected**: Fraud alert modal appears with HIGH risk

### Test Case 2: Rapid-Fire Transactions
1. Create 5 transactions within 2 minutes
2. Each with amount $50-100
3. **Expected**: 5th transaction shows fraud alert with rapid-fire reason

### Test Case 3: Geographical Anomaly
1. Create transaction in "New York"
2. Immediately create another in "Los Angeles"
3. **Expected**: Second transaction shows fraud alert with location reason

### Test Case 4: Normal Transaction
1. Create transaction with normal amount ($50)
2. Common category (groceries)
3. **Expected**: No fraud alert, transaction created normally

## API Response Example

### Fraudulent Transaction Response
```json
{
  "id": 123,
  "amount": 10000.00,
  "type": "EXPENSE",
  "category": "luxury_electronics",
  "description": "Expensive purchase",
  "location": "New York",
  "transactionDate": "2024-02-25T10:30:00",
  "fraudulent": true,
  "fraudScore": 75.0,
  "riskLevel": "HIGH",
  "status": "FLAGGED",
  "fraudReasons": [
    "Amount $10000.00 exceeds 3x user average $150.00",
    "First time using category: luxury_electronics",
    "5 or more transactions within 10 minutes"
  ]
}
```

### Normal Transaction Response
```json
{
  "id": 124,
  "amount": 50.00,
  "type": "EXPENSE",
  "category": "groceries",
  "description": "Weekly shopping",
  "location": "Local Store",
  "transactionDate": "2024-02-25T11:00:00",
  "fraudulent": false,
  "fraudScore": 0.0,
  "riskLevel": "LOW",
  "status": "COMPLETED",
  "fraudReasons": null
}
```

## Styling Details

### Color Scheme
- **Fraud Score Section**: Red-to-yellow gradient (#fee2e2 to #fef3c7)
- **Transaction Summary**: Light gray background (#f9fafb)
- **Fraud Reasons**: Yellow background (#fef3c7) with orange border
- **Info Message**: Blue background (#eff6ff) with blue border

### Risk Level Colors
- **LOW**: Yellow (#fbbf24)
- **MEDIUM**: Orange (#f97316)
- **HIGH**: Red (#dc2626)
- **CRITICAL**: Dark red (#991b1b)

## Future Enhancements

1. **Sound Alert**: Play notification sound when fraud detected
2. **Animation**: Shake effect for high-risk alerts
3. **Email Notification**: Send email for critical fraud alerts
4. **SMS Alert**: Optional SMS for high-value fraudulent transactions
5. **Fraud History**: Show similar past fraud patterns
6. **Quick Actions**: Add "Mark as Safe" button in modal
7. **Detailed Analytics**: Link to detailed fraud analysis page

## Troubleshooting

### Modal Not Appearing
- Check browser console for errors
- Verify `fraudScore > 0` in response
- Check `showFraudModal` state in React DevTools

### Navigation Not Working
- Verify React Router is properly configured
- Check `/fraud-alerts` route exists
- Ensure `useNavigate` hook is imported

### Styling Issues
- Clear browser cache
- Check CSS file is imported
- Verify no CSS conflicts with other components

## Accessibility

- Modal can be closed with Escape key (add this feature)
- Focus trap within modal (add this feature)
- ARIA labels for screen readers (add this feature)
- Keyboard navigation support (add this feature)
