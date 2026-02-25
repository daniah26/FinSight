import React, { useState, useEffect } from 'react';
import { getDashboardSummary } from '../services/api';
import Card from '../components/Card';
import './Dashboard.css';

const Dashboard = ({ userId }) => {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboard();
  }, [userId]);

  const loadDashboard = async () => {
    try {
      setLoading(true);
      const response = await getDashboardSummary(userId);
      setSummary(response.data);
    } catch (error) {
      console.error('Error loading dashboard:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount || 0);
  };

  if (loading) {
    return <div className="loading">Loading dashboard...</div>;
  }

  if (!summary) {
    return <div className="error">Failed to load dashboard</div>;
  }

  return (
    <div className="dashboard">
      <h1 className="page-title">Dashboard</h1>
      
      <div className="stats-grid">
        <Card className="stat-card stat-income">
          <div className="stat-icon">💵</div>
          <div className="stat-content">
            <div className="stat-label">Total Income</div>
            <div className="stat-value">{formatCurrency(summary.totalIncome)}</div>
          </div>
        </Card>

        <Card className="stat-card stat-expenses">
          <div className="stat-icon">💸</div>
          <div className="stat-content">
            <div className="stat-label">Total Expenses</div>
            <div className="stat-value">{formatCurrency(summary.totalExpenses)}</div>
          </div>
        </Card>

        <Card className="stat-card stat-balance">
          <div className="stat-icon">💰</div>
          <div className="stat-content">
            <div className="stat-label">Current Balance</div>
            <div className="stat-value">{formatCurrency(summary.currentBalance)}</div>
          </div>
        </Card>

        <Card className="stat-card stat-fraud">
          <div className="stat-icon">⚠️</div>
          <div className="stat-content">
            <div className="stat-label">Flagged Transactions</div>
            <div className="stat-value">{summary.totalFlaggedTransactions}</div>
            <div className="stat-subtext">
              Avg Score: {summary.averageFraudScore?.toFixed(1) || 0}
            </div>
          </div>
        </Card>
      </div>

      <div className="charts-grid">
        <Card title="Spending by Category">
          <div className="category-list">
            {Object.entries(summary.spendingByCategory || {}).map(([category, amount]) => (
              <div key={category} className="category-item">
                <div className="category-info">
                  <span className="category-name">{category}</span>
                  <span className="category-amount">{formatCurrency(amount)}</span>
                </div>
                <div className="category-bar">
                  <div 
                    className="category-bar-fill"
                    style={{ 
                      width: `${(amount / Math.max(...Object.values(summary.spendingByCategory))) * 100}%` 
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </Card>

        <Card title="Fraud by Category">
          <div className="fraud-list">
            {Object.entries(summary.fraudByCategory || {}).length > 0 ? (
              Object.entries(summary.fraudByCategory).map(([category, count]) => (
                <div key={category} className="fraud-item">
                  <span className="fraud-category">{category}</span>
                  <span className="fraud-count">{count} incidents</span>
                </div>
              ))
            ) : (
              <div className="empty-state">No fraud incidents detected</div>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
};

export default Dashboard;
