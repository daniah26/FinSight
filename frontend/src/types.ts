// src/types/api.ts (or wherever you keep shared types)

export type TransactionType = "INCOME" | "EXPENSE";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type AlertSeverity = "LOW" | "MEDIUM" | "HIGH";
export type SubscriptionStatus = "ACTIVE" | "IGNORED";

// --------------------
// Transactions
// --------------------

// Request body for POST /api/transactions
export interface TransactionRequest {
  userId: number;
  amount: number;
  type: TransactionType;
  category: string;
  description?: string;
  location?: string;
  transactionDate: string; // ISO string
}

// Response for GET/POST /api/transactions
export interface TransactionResponse {
  id: number;
  amount: number;
  type: TransactionType;
  category: string;
  description?: string;
  location?: string;
  transactionDate: string; // ISO string

  fraudScore: number;
  riskLevel: RiskLevel;
  fraudulent: boolean;
  status?: string;
  fraudReasons?: string[];
}

// Spring Page wrapper for GET /api/transactions (because spec says Page<TransactionResponse>)
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // current page index
  first: boolean;
  last: boolean;
}

// --------------------
// Dashboard
// --------------------

export interface TimeSeriesPoint {
  date: string; // LocalDate from backend
  amount: number; // BigDecimal from backend
}

export interface DashboardSummary {
  totalIncome: number;
  totalExpenses: number;
  currentBalance: number;
  totalFlaggedTransactions: number;
  averageFraudScore: number;
  spendingByCategory: Record<string, number>; // Map<String, BigDecimal>
  fraudByCategory: Record<string, number>; // Map<String, Long>
  spendingTrends: TimeSeriesPoint[];
}

// --------------------
// Fraud Alerts
// --------------------

export interface FraudAlertDto {
  id: number;
  userId: number;
  message: string;
  severity: AlertSeverity;
  resolved: boolean;
  createdAt: string; // ISO string

  // Spec: includes embedded TransactionResponse
  transaction: TransactionResponse;
}

// --------------------
// Subscriptions
// --------------------

export interface SubscriptionDto {
  id: number;
  merchant: string;       // category name
  avgAmount: number;
  lastPaidDate: string;   // ISO string
  nextDueDate: string;    // ISO string
  status: SubscriptionStatus; // ACTIVE | IGNORED
  createdAt: string;      // ISO string
}

// --------------------
// Error Response (all error cases)
// --------------------

export interface ErrorResponse {
  status: number;        // e.g. 404
  error: string;         // e.g. "Not Found"
  message: string;       // details
  timestamp: string;     // ISO string
}