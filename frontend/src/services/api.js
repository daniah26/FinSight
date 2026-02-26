import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Transactions
export const getTransactions = (userId, params = {}) => {
  return api.get('/transactions', { params: { userId, ...params } });
};

export const getAllTransactions = async (userId, params = {}) => {
  // Fetch with a large size to get all transactions
  return api.get('/transactions', { 
    params: { 
      userId, 
      ...params,
      size: 10000, // Large number to get all transactions
      page: 0 
    } 
  });
};

export const createTransaction = (data) => {
  return api.post('/transactions', data);
};

// Dashboard
export const getDashboardSummary = (userId, startDate, endDate) => {
  return api.get('/summary', { params: { userId, startDate, endDate } });
};

// Fraud Alerts
export const getFraudAlerts = (userId, resolved, severity) => {
  return api.get('/fraud/alerts', { params: { userId, resolved, severity } });
};

export const resolveAlert = (alertId, userId) => {
  return api.put(`/fraud/alerts/${alertId}/resolve`, null, { params: { userId } });
};

// Subscriptions
export const getSubscriptions = (userId) => {
  return api.get('/subscriptions', { params: { userId } });
};

export const getDueSoonSubscriptions = (userId, days = 7) => {
  return api.get('/subscriptions/due-soon', { params: { userId, days } });
};

export const ignoreSubscription = (subscriptionId, userId) => {
  return api.put(`/subscriptions/${subscriptionId}/ignore`, null, { params: { userId } });
};

export default api;
