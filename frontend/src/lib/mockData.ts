// Type definitions for FinSight data (used across pages)

export interface Transaction {
  id: number;
  amount: number;
  type: 'INCOME' | 'EXPENSE';
  category: string;
  description: string;
  location: string;
  transactionDate: string;
  fraudulent: boolean;
  fraudScore: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
}

export interface DashboardSummary {
  totalIncome: number;
  totalExpenses: number;
  currentBalance: number;
  totalFlaggedTransactions: number;
  averageFraudScore: number;
  spendingByCategory: Record<string, number>;
  fraudByCategory: Record<string, number>;
}

export interface FraudAlert {
  id: number;
  message: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  resolved: boolean;
  createdAt: string;
  transaction: {
    category: string;
    amount: number;
    fraudScore: number;
    description: string;
  };
}

export interface Subscription {
  id: number;
  merchant: string;
  avgAmount: number;
  lastPaidDate: string;
  nextDueDate: string;
  status: 'ACTIVE' | 'IGNORED';
}
