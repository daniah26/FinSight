import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8389/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Auth interceptor
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auth
export const registerUser = (data: { username: string; email: string; password: string }) =>
  api.post('/auth/signup', data);

export const loginUser = (data: { username: string; password: string }) =>
  api.post('/auth/login', data);

// Transactions
export const getTransactions = (userId: number, params: Record<string, unknown> = {}) =>
  api.get('/transactions', { params: { userId, ...params } });

export const getAllTransactions = (userId: number, params: Record<string, unknown> = {}) =>
  api.get('/transactions', {
    params: {
      userId,
      ...params,
      size: 10000,
      page: 0,
    },
  });

export const createTransaction = (data: {
  amount: number;
  type: string;
  category: string;
  description?: string;
  location?: string;
  transactionDate: string;
  userId: number;
}) => api.post('/transactions', data);

// Dashboard
export const getDashboardSummary = (userId: number, startDate?: string, endDate?: string) =>
  api.get('/summary', { params: { userId, startDate, endDate } });

// Fraud Alerts
export const getFraudAlerts = (userId: number, resolved?: boolean, severity?: string) =>
  api.get('/fraud/alerts', { params: { userId, resolved, severity } });

export const resolveAlert = (alertId: number, userId: number) =>
  api.put(`/fraud/alerts/${alertId}/resolve`, null, { params: { userId } });

// Subscriptions
export const getSubscriptions = (userId: number) =>
  api.get('/subscriptions', { params: { userId } });

export const detectSubscriptions = (userId: number) =>
  api.post('/subscriptions/detect', null, { params: { userId } });

export const createSubscription = (userId: number, data: {
  merchant: string;
  avgAmount: number;
  lastPaidDate: string;
  nextDueDate: string;
}) => api.post('/subscriptions', data, { params: { userId } });

export const getDueSoonSubscriptions = (userId: number, days: number = 7) =>
  api.get('/subscriptions/due-soon', { params: { userId, days } });

export const ignoreSubscription = (subscriptionId: number, userId: number) =>
  api.put(`/subscriptions/${subscriptionId}/ignore`, null, { params: { userId } });

export default api;
