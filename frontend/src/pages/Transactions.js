import React, { useState, useEffect } from 'react';
import { getTransactions, createTransaction } from '../services/api';
import Card from '../components/Card';
import Button from '../components/Button';
import Badge from '../components/Badge';
import './Transactions.css';

const Transactions = ({ userId }) => {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [filters, setFilters] = useState({
    type: '',
    category: '',
    fraudulent: ''
  });
  const [formData, setFormData] = useState({
    amount: '',
    type: 'EXPENSE',
    category: '',
    description: '',
    location: '',
    transactionDate: new Date().toISOString().slice(0, 16)
  });

  useEffect(() => {
    loadTransactions();
  }, [userId, filters]);

  const loadTransactions = async () => {
    try {
      setLoading(true);
      const params = {
        ...filters,
        fraudulent: filters.fraudulent === '' ? undefined : filters.fraudulent === 'true'
      };
      const response = await getTransactions(userId, params);
      setTransactions(response.data.content || []);
    } catch (error) {
      console.error('Error loading transactions:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await createTransaction({
        userId,
        ...formData,
        amount: parseFloat(formData.amount)
      });
      setShowForm(false);
      setFormData({
        amount: '',
        type: 'EXPENSE',
        category: '',
        description: '',
        location: '',
        transactionDate: new Date().toISOString().slice(0, 16)
      });
      loadTransactions();
    } catch (error) {
      console.error('Error creating transaction:', error);
      alert('Failed to create transaction');
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount || 0);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getRiskBadge = (riskLevel) => {
    const variants = {
      LOW: 'low',
      MEDIUM: 'medium',
      HIGH: 'high'
    };
    return <Badge variant={variants[riskLevel] || 'default'}>{riskLevel}</Badge>;
  };

  return (
    <div className="transactions">
      <div className="page-header">
        <h1 className="page-title">Transactions</h1>
        <Button onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Cancel' : '+ Add Transaction'}
        </Button>
      </div>

      {showForm && (
        <Card title="New Transaction" className="transaction-form-card">
          <form onSubmit={handleSubmit} className="transaction-form">
            <div className="form-row">
              <div className="form-group">
                <label>Amount *</label>
                <input
                  type="number"
                  step="0.01"
                  required
                  value={formData.amount}
                  onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                  placeholder="0.00"
                />
              </div>
              <div className="form-group">
                <label>Type *</label>
                <select
                  value={formData.type}
                  onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                >
                  <option value="EXPENSE">Expense</option>
                  <option value="INCOME">Income</option>
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label>Category *</label>
                <input
                  type="text"
                  required
                  value={formData.category}
                  onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                  placeholder="e.g., groceries, salary"
                />
              </div>
              <div className="form-group">
                <label>Date *</label>
                <input
                  type="datetime-local"
                  required
                  value={formData.transactionDate}
                  onChange={(e) => setFormData({ ...formData, transactionDate: e.target.value })}
                />
              </div>
            </div>

            <div className="form-group">
              <label>Description</label>
              <input
                type="text"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="Optional description"
              />
            </div>

            <div className="form-group">
              <label>Location</label>
              <input
                type="text"
                value={formData.location}
                onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                placeholder="Optional location"
              />
            </div>

            <div className="form-actions">
              <Button type="submit" variant="primary">Create Transaction</Button>
              <Button type="button" variant="secondary" onClick={() => setShowForm(false)}>
                Cancel
              </Button>
            </div>
          </form>
        </Card>
      )}

      <Card title="Filters" className="filters-card">
        <div className="filters">
          <select
            value={filters.type}
            onChange={(e) => setFilters({ ...filters, type: e.target.value })}
          >
            <option value="">All Types</option>
            <option value="INCOME">Income</option>
            <option value="EXPENSE">Expense</option>
          </select>

          <input
            type="text"
            placeholder="Category"
            value={filters.category}
            onChange={(e) => setFilters({ ...filters, category: e.target.value })}
          />

          <select
            value={filters.fraudulent}
            onChange={(e) => setFilters({ ...filters, fraudulent: e.target.value })}
          >
            <option value="">All Transactions</option>
            <option value="true">Fraudulent Only</option>
            <option value="false">Non-Fraudulent</option>
          </select>

          <Button 
            variant="secondary" 
            size="small"
            onClick={() => setFilters({ type: '', category: '', fraudulent: '' })}
          >
            Clear Filters
          </Button>
        </div>
      </Card>

      <Card title={`Transactions (${transactions.length})`}>
        {loading ? (
          <div className="loading">Loading transactions...</div>
        ) : transactions.length === 0 ? (
          <div className="empty-state">No transactions found</div>
        ) : (
          <div className="transactions-list">
            {transactions.map((transaction) => (
              <div key={transaction.id} className="transaction-item">
                <div className="transaction-main">
                  <div className="transaction-icon">
                    {transaction.type === 'INCOME' ? '💵' : '💸'}
                  </div>
                  <div className="transaction-details">
                    <div className="transaction-category">{transaction.category}</div>
                    <div className="transaction-description">
                      {transaction.description || 'No description'}
                    </div>
                    <div className="transaction-date">{formatDate(transaction.transactionDate)}</div>
                  </div>
                </div>
                <div className="transaction-right">
                  <div className={`transaction-amount ${transaction.type.toLowerCase()}`}>
                    {transaction.type === 'INCOME' ? '+' : '-'}
                    {formatCurrency(transaction.amount)}
                  </div>
                  <div className="transaction-badges">
                    {transaction.fraudulent && (
                      <>
                        {getRiskBadge(transaction.riskLevel)}
                        <Badge variant="danger">Fraud: {transaction.fraudScore?.toFixed(0)}</Badge>
                      </>
                    )}
                    <Badge variant={transaction.type === 'INCOME' ? 'success' : 'info'}>
                      {transaction.type}
                    </Badge>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
};

export default Transactions;
