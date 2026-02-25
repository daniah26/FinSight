import React, { useState, useEffect } from 'react';
import { getSubscriptions, getDueSoonSubscriptions, ignoreSubscription } from '../services/api';
import Card from '../components/Card';
import Button from '../components/Button';
import Badge from '../components/Badge';
import './Subscriptions.css';

const Subscriptions = ({ userId }) => {
  const [subscriptions, setSubscriptions] = useState([]);
  const [dueSoon, setDueSoon] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadSubscriptions();
  }, [userId]);

  const loadSubscriptions = async () => {
    try {
      setLoading(true);
      const [subsResponse, dueResponse] = await Promise.all([
        getSubscriptions(userId),
        getDueSoonSubscriptions(userId, 7)
      ]);
      setSubscriptions(subsResponse.data || []);
      setDueSoon(dueResponse.data || []);
    } catch (error) {
      console.error('Error loading subscriptions:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleIgnore = async (subscriptionId) => {
    try {
      await ignoreSubscription(subscriptionId, userId);
      loadSubscriptions();
    } catch (error) {
      console.error('Error ignoring subscription:', error);
      alert('Failed to ignore subscription');
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
      day: 'numeric'
    });
  };

  const getDaysUntil = (dateString) => {
    const date = new Date(dateString);
    const today = new Date();
    const diffTime = date - today;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  };

  return (
    <div className="subscriptions">
      <h1 className="page-title">Subscriptions</h1>

      {dueSoon.length > 0 && (
        <div className="due-soon-banner">
          <div className="banner-icon">⏰</div>
          <div className="banner-content">
            <div className="banner-title">Upcoming Payments</div>
            <div className="banner-text">
              You have {dueSoon.length} subscription{dueSoon.length > 1 ? 's' : ''} due within 7 days
            </div>
          </div>
        </div>
      )}

      <Card title={`All Subscriptions (${subscriptions.length})`}>
        {loading ? (
          <div className="loading">Loading subscriptions...</div>
        ) : subscriptions.length === 0 ? (
          <div className="empty-state">No subscriptions detected</div>
        ) : (
          <div className="subscriptions-grid">
            {subscriptions.map((subscription) => {
              const daysUntil = getDaysUntil(subscription.nextDueDate);
              const isDueSoon = daysUntil <= 7 && daysUntil >= 0;
              
              return (
                <div 
                  key={subscription.id} 
                  className={`subscription-card ${isDueSoon ? 'due-soon' : ''} ${subscription.status === 'IGNORED' ? 'ignored' : ''}`}
                >
                  <div className="subscription-header">
                    <div className="subscription-icon">📱</div>
                    <div className="subscription-badges">
                      {subscription.status === 'IGNORED' ? (
                        <Badge variant="default">Ignored</Badge>
                      ) : isDueSoon ? (
                        <Badge variant="warning">Due Soon</Badge>
                      ) : (
                        <Badge variant="success">Active</Badge>
                      )}
                    </div>
                  </div>

                  <div className="subscription-merchant">{subscription.merchant}</div>
                  
                  <div className="subscription-amount">
                    {formatCurrency(subscription.avgAmount)}
                    <span className="subscription-frequency">/month</span>
                  </div>

                  <div className="subscription-dates">
                    <div className="subscription-date-item">
                      <span className="date-label">Last Paid:</span>
                      <span className="date-value">{formatDate(subscription.lastPaidDate)}</span>
                    </div>
                    <div className="subscription-date-item">
                      <span className="date-label">Next Due:</span>
                      <span className={`date-value ${isDueSoon ? 'due-soon-text' : ''}`}>
                        {formatDate(subscription.nextDueDate)}
                        {isDueSoon && ` (${daysUntil} days)`}
                      </span>
                    </div>
                  </div>

                  {subscription.status === 'ACTIVE' && (
                    <div className="subscription-actions">
                      <Button 
                        variant="secondary" 
                        size="small"
                        onClick={() => handleIgnore(subscription.id)}
                      >
                        Ignore
                      </Button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </Card>
    </div>
  );
};

export default Subscriptions;
