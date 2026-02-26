import React, { useState, useEffect } from 'react';
import { getTransactions, createTransaction } from '../services/api';
import Card from '../components/Card';
import Button from '../components/Button';
import Badge from '../components/Badge';
import FraudAlertModal from '../components/FraudAlertModal';
import './Transactions.css';

const Transactions = ({ userId }) => {
  const [transactions, setTransactions] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [showFraudModal, setShowFraudModal] = useState(false);
  const [fraudData, setFraudData] = useState(null);
  const [dateFilter, setDateFilter] = useState('all');
  const [sortBy, setSortBy] = useState('transactionDate');
  const [sortDir, setSortDir] = useState('DESC');
  const [filters, setFilters] = useState({
    type: '',
    category: '',
    fraudulent: '',
    startDate: '',
    endDate: ''
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
    setCurrentPage(0); // Reset to first page when filters change
  }, [filters, sortBy, sortDir, dateFilter]);

  useEffect(() => {
    loadTransactions();
  }, [userId, filters, currentPage, sortBy, sortDir, dateFilter]);

  const getDateRange = () => {
    const now = new Date();
    let startDate = null;
    let endDate = null;

    switch (dateFilter) {
      case 'last7':
        startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
        endDate = now;
        break;
      case 'last30':
        startDate = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
        endDate = now;
        break;
      case 'thisMonth':
        startDate = new Date(now.getFullYear(), now.getMonth(), 1);
        endDate = now;
        break;
      case 'lastMonth':
        startDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
        endDate = new Date(now.getFullYear(), now.getMonth(), 0);
        break;
      case 'custom':
        startDate = filters.startDate ? new Date(filters.startDate) : null;
        endDate = filters.endDate ? new Date(filters.endDate) : null;
        break;
      default:
        return { startDate: null, endDate: null };
    }

    return {
      startDate: startDate ? startDate.toISOString() : null,
      endDate: endDate ? endDate.toISOString() : null
    };
  };

  const loadTransactions = async () => {
    try {
      setLoading(true);
      const dateRange = getDateRange();
      const params = {
        type: filters.type || undefined,
        category: filters.category || undefined,
        fraudulent: filters.fraudulent === '' ? undefined : filters.fraudulent === 'true',
        startDate: dateRange.startDate,
        endDate: dateRange.endDate,
        sortBy,
        sortDir,
        page: currentPage,
        size: 20
      };
      const response = await getTransactions(userId, params);
      setTransactions(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
      setTotalElements(response.data.totalElements || 0);
    } catch (error) {
      console.error('Error loading transactions:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await createTransaction({
        userId,
        ...formData,
        amount: parseFloat(formData.amount)
      });
      
      const transaction = response.data;
      // Only show fraud alert popup for MEDIUM and HIGH risk (score >= 40)
      if (transaction.fraudulent && transaction.fraudScore >= 40) {
        setFraudData({
          amount: transaction.amount,
          category: transaction.category,
          description: transaction.description,
          location: transaction.location,
          fraudScore: transaction.fraudScore,
          riskLevel: transaction.riskLevel,
          reasons: transaction.fraudReasons || []
        });
        setShowFraudModal(true);
      }
      
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

  const handleSort = (field) => {
    if (sortBy === field) {
      setSortDir(sortDir === 'ASC' ? 'DESC' : 'ASC');
    } else {
      setSortBy(field);
      setSortDir('DESC');
    }
  };

  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  const renderPagination = () => {
    if (totalPages <= 1) return null;

    const pages = [];
    const maxVisible = 5;
    let startPage = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxVisible - 1);

    if (endPage - startPage < maxVisible - 1) {
      startPage = Math.max(0, endPage - maxVisible + 1);
    }

    return (
      <div className="pagination">
        <button
          className="pagination-btn"
          onClick={() => handlePageChange(0)}
          disabled={currentPage === 0}
        >
          &laquo;
        </button>
        <button
          className="pagination-btn"
          onClick={() => handlePageChange(currentPage - 1)}
          disabled={currentPage === 0}
        >
          &lsaquo;
        </button>

        {startPage > 0 && (
          <>
            <button className="pagination-btn" onClick={() => handlePageChange(0)}>
              1
            </button>
            {startPage > 1 && <span className="pagination-ellipsis">...</span>}
          </>
        )}

        {Array.from({ length: endPage - startPage + 1 }, (_, i) => startPage + i).map((page) => (
          <button
            key={page}
            className={`pagination-btn ${currentPage === page ? 'active' : ''}`}
            onClick={() => handlePageChange(page)}
          >
            {page + 1}
          </button>
        ))}

        {endPage < totalPages - 1 && (
          <>
            {endPage < totalPages - 2 && <span className="pagination-ellipsis">...</span>}
            <button className="pagination-btn" onClick={() => handlePageChange(totalPages - 1)}>
              {totalPages}
            </button>
          </>
        )}

        <button
          className="pagination-btn"
          onClick={() => handlePageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
        >
          &rsaquo;
        </button>
        <button
          className="pagination-btn"
          onClick={() => handlePageChange(totalPages - 1)}
          disabled={currentPage >= totalPages - 1}
        >
          &raquo;
        </button>
      </div>
    );
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
      <FraudAlertModal
        isOpen={showFraudModal}
        onClose={() => setShowFraudModal(false)}
        fraudData={fraudData}
      />
      
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
          <div className="filter-group">
            <label>Date Range</label>
            <select
              value={dateFilter}
              onChange={(e) => setDateFilter(e.target.value)}
              className="filter-select"
            >
              <option value="all">All Time</option>
              <option value="last7">Last 7 Days</option>
              <option value="last30">Last 30 Days</option>
              <option value="thisMonth">This Month</option>
              <option value="lastMonth">Last Month</option>
              <option value="custom">Custom Range</option>
            </select>
          </div>

          {dateFilter === 'custom' && (
            <>
              <div className="filter-group">
                <label>Start Date</label>
                <input
                  type="date"
                  value={filters.startDate}
                  onChange={(e) => setFilters({ ...filters, startDate: e.target.value })}
                  className="filter-input"
                />
              </div>
              <div className="filter-group">
                <label>End Date</label>
                <input
                  type="date"
                  value={filters.endDate}
                  onChange={(e) => setFilters({ ...filters, endDate: e.target.value })}
                  className="filter-input"
                />
              </div>
            </>
          )}

          <div className="filter-group">
            <label>Type</label>
            <select
              value={filters.type}
              onChange={(e) => setFilters({ ...filters, type: e.target.value })}
              className="filter-select"
            >
              <option value="">All Types</option>
              <option value="INCOME">Income</option>
              <option value="EXPENSE">Expense</option>
            </select>
          </div>

          <div className="filter-group">
            <label>Category</label>
            <input
              type="text"
              placeholder="Search categories..."
              value={filters.category}
              onChange={(e) => setFilters({ ...filters, category: e.target.value })}
              className="filter-input"
            />
          </div>

          <div className="filter-group">
            <label>Status</label>
            <select
              value={filters.fraudulent}
              onChange={(e) => setFilters({ ...filters, fraudulent: e.target.value })}
              className="filter-select"
            >
              <option value="">All Transactions</option>
              <option value="true">Fraudulent Only</option>
              <option value="false">Non-Fraudulent</option>
            </select>
          </div>

          <div className="filter-group">
            <label>&nbsp;</label>
            <Button 
              variant="secondary" 
              size="small"
              onClick={() => {
                setFilters({ type: '', category: '', fraudulent: '', startDate: '', endDate: '' });
                setDateFilter('all');
              }}
            >
              Clear All
            </Button>
          </div>
        </div>

        <div className="sort-controls">
          <span>Sort by:</span>
          <button
            className={`sort-btn ${sortBy === 'transactionDate' ? 'active' : ''}`}
            onClick={() => handleSort('transactionDate')}
          >
            Date {sortBy === 'transactionDate' && (sortDir === 'ASC' ? '↑' : '↓')}
          </button>
          <button
            className={`sort-btn ${sortBy === 'amount' ? 'active' : ''}`}
            onClick={() => handleSort('amount')}
          >
            Amount {sortBy === 'amount' && (sortDir === 'ASC' ? '↑' : '↓')}
          </button>
          <button
            className={`sort-btn ${sortBy === 'category' ? 'active' : ''}`}
            onClick={() => handleSort('category')}
          >
            Category {sortBy === 'category' && (sortDir === 'ASC' ? '↑' : '↓')}
          </button>
        </div>
      </Card>

      <Card title={`Transactions (${totalElements} total, showing page ${currentPage + 1} of ${totalPages || 1})`}>
        {loading ? (
          <div className="loading">Loading transactions...</div>
        ) : transactions.length === 0 ? (
          <div className="empty-state">No transactions found</div>
        ) : (
          <>
            <div className="transactions-list">
              {transactions.map((transaction) => (
                <div 
                  key={transaction.id} 
                  className={`transaction-item ${transaction.fraudulent ? 'fraudulent' : ''}`}
                >
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
            {renderPagination()}
          </>
        )}
      </Card>
    </div>
  );
};

export default Transactions;
