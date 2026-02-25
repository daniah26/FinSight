import React, { useState, useEffect } from 'react';
import { getFraudAlerts, resolveAlert } from '../services/api';
import Card from '../components/Card';
import Button from '../components/Button';
import Badge from '../components/Badge';
import './FraudAlerts.css';

const FraudAlerts = ({ userId }) => {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState({ resolved: '', severity: '' });

  useEffect(() => {
    loadAlerts();
  }, [userId, filter]);

  const loadAlerts = async () => {
    try {
      setLoading(true);
      const params = {
        resolved: filter.resolved === '' ? undefined : filter.resolved === 'true',
        severity: filter.severity || undefined
      };
      const response = await getFraudAlerts(userId, params.resolved, params.severity);
      setAlerts(response.data || []);
    } catch (error) {
      console.error('Error loading alerts:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleResolve = async (alertId) => {
    try {
      await resolveAlert(alertId, userId);
      loadAlerts();
    } catch (error) {
      console.error('Error resolving alert:', error);
      alert('Failed to resolve alert');
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

  return (
    <div className="fraud-alerts">
      <h1 className="page-title">Fraud Alerts</h1>

      <Card title="Filters" className="filters-card">
        <div className="filters">
          <select
            value={filter.resolved}
            onChange={(e) => setFilter({ ...filter, resolved: e.target.value })}
          >
            <option value="">All Alerts</option>
            <option value="false">Unresolved</option>
            <option value="true">Resolved</option>
          </select>

          <select
            value={filter.severity}
            onChange={(e) => setFilter({ ...filter, severity: e.target.value })}
          >
            <option value="">All Severities</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </select>

          <Button 
            variant="secondary" 
            size="small"
            onClick={() => setFilter({ resolved: '', severity: '' })}
          >
            Clear Filters
          </Button>
        </div>
      </Card>

      <Card title={`Alerts (${alerts.length})`}>
        {loading ? (
          <div className="loading">Loading alerts...</div>
        ) : alerts.length === 0 ? (
          <div className="empty-state">No fraud alerts found</div>
        ) : (
          <div className="alerts-list">
            {alerts.map((alert) => (
              <div key={alert.id} className={`alert-item ${alert.resolved ? 'resolved' : ''}`}>
                <div className="alert-header">
                  <div className="alert-badges">
                    <Badge variant={alert.severity.toLowerCase()}>{alert.severity}</Badge>
                    {alert.resolved && <Badge variant="success">Resolved</Badge>}
                    <Badge variant="danger">Score: {alert.transaction.fraudScore?.toFixed(0)}</Badge>
                  </div>
                  <div className="alert-date">{formatDate(alert.createdAt)}</div>
                </div>

                <div className="alert-message">{alert.message}</div>

                <div className="alert-transaction">
                  <div className="alert-transaction-info">
                    <span className="alert-transaction-category">
                      {alert.transaction.category}
                    </span>
                    <span className="alert-transaction-amount">
                      {formatCurrency(alert.transaction.amount)}
                    </span>
                  </div>
                  <div className="alert-transaction-details">
                    {alert.transaction.description} • {formatDate(alert.transaction.transactionDate)}
                  </div>
                </div>

                {!alert.resolved && (
                  <div className="alert-actions">
                    <Button 
                      variant="success" 
                      size="small"
                      onClick={() => handleResolve(alert.id)}
                    >
                      Mark as Resolved
                    </Button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
};

export default FraudAlerts;
