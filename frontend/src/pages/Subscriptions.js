import React, { useState, useEffect } from 'react';
import { getDueSoonSubscriptions, ignoreSubscription, detectSubscriptions } from '../services/api';
import Card from '../components/Card';
import Button from '../components/Button';
import Badge from '../components/Badge';
import './Subscriptions.css';

const Subscriptions = ({ userId }) => {
  const [subscriptions, setSubscriptions] = useState([]);
  const [dueSoon, setDueSoon] = useState([]);
  const [loading, setLoading] = useState(true);
  const [confirmIgnore, setConfirmIgnore] = useState(null);

  useEffect(() => {
    loadSubscriptions();
  }, [userId]);

  const loadSubscriptions = async () => {
    try {
      setLoading(true);

      // Step 1: Run detection first (saves to DB)
      const subsResponse = await detectSubscriptions(userId);
      const newSubs = Array.isArray(subsResponse.data) ? subsResponse.data : [];

      // Step 2: THEN fetch due-soon (reads the freshly-saved data)
      const dueResponse = await getDueSoonSubscriptions(userId, 7);
      const newDue = Array.isArray(dueResponse.data) ? dueResponse.data : [];

      setSubscriptions(newSubs);
      setDueSoon(newDue);

      console.log(`Subscriptions loaded: ${newSubs.length} total, ${newDue.length} due soon`);
    } catch (error) {
      console.error('Error loading subscriptions:', error);
      setSubscriptions([]);
      setDueSoon([]);
    } finally {
      setLoading(false);
    }
  };

  const handleIgnoreClick = (subscription) => {
    setConfirmIgnore(subscription);
  };

  const handleConfirmIgnore = async () => {
    if (!confirmIgnore) return;
    try {
      await ignoreSubscription(confirmIgnore.id, userId);
      setConfirmIgnore(null);
      loadSubscriptions();
    } catch (error) {
      console.error('Error ignoring subscription:', error);
      alert('Failed to ignore subscription');
    }
  };

  const handleCancelIgnore = () => {
    setConfirmIgnore(null);
  };

  const handleRefresh = async () => {
    try {
      setLoading(true);
      setSubscriptions([]);
      setDueSoon([]);

      const subsResponse = await detectSubscriptions(userId);
      const newSubs = Array.isArray(subsResponse.data) ? subsResponse.data : [];

      // Sequential: due-soon after detection completes
      const dueResponse = await getDueSoonSubscriptions(userId, 7);
      const newDue = Array.isArray(dueResponse.data) ? dueResponse.data : [];

      setSubscriptions(newSubs);
      setDueSoon(newDue);

      console.log(`Subscriptions refreshed: ${newSubs.length} total, ${newDue.length} due soon`);
    } catch (error) {
      console.error('Error refreshing subscriptions:', error);
      alert('Failed to refresh subscriptions');
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

  const formatDate = (dateString) => {
    // Parse as local date to avoid UTC timezone shift (e.g. "2025-03-01" → Mar 1, not Feb 28)
    const [year, month, day] = dateString.split('T')[0].split('-').map(Number);
    return new Date(year, month - 1, day).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  // Returns days until due date, using local date comparison to avoid timezone bugs
  const getDaysUntil = (dateString) => {
    const [year, month, day] = dateString.split('T')[0].split('-').map(Number);
    const dueDate = new Date(year, month - 1, day);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const diffTime = dueDate - today;
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  };

  const activeSubscriptions = subscriptions.filter(sub => sub.status !== 'IGNORED');

  return (
    <div className="subscriptions">
      <div className="page-header">
        <h1 className="page-title">Subscriptions</h1>
        <Button variant="secondary" onClick={handleRefresh} disabled={loading}>
          🔄 Refresh
        </Button>
      </div>

      {confirmIgnore && (
        <div className="modal-overlay" onClick={handleCancelIgnore}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Ignore Subscription?</h2>
            <p>Are you sure you want to ignore <strong>{confirmIgnore.merchant}</strong>?</p>
            <p className="modal-note">
              This subscription will be hidden from your list and you won't receive alerts for it.
            </p>
            <div className="modal-actions">
              <Button variant="secondary" onClick={handleCancelIgnore}>Cancel</Button>
              <Button variant="danger" onClick={handleConfirmIgnore}>Ignore Subscription</Button>
            </div>
          </div>
        </div>
      )}

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

      <Card title={`All Subscriptions (${activeSubscriptions.length})`}>
        {loading ? (
          <div className="loading">Detecting subscriptions…</div>
        ) : activeSubscriptions.length === 0 ? (
          <div className="empty-state">
            No subscriptions detected. Add two EXPENSE transactions with the same category
            in two consecutive months to get started.
          </div>
        ) : (
          <div className="subscriptions-grid">
            {activeSubscriptions.map((subscription) => {
              const daysUntil = getDaysUntil(subscription.nextDueDate);
              const isDueSoon = daysUntil >= 0 && daysUntil <= 7;

              return (
                <div
                  key={subscription.id}
                  className={`subscription-card ${isDueSoon ? 'due-soon' : ''}`}
                >
                  <div className="subscription-header">
                    <div className="subscription-icon">📱</div>
                    <div className="subscription-badges">
                      {isDueSoon ? (
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
                        {isDueSoon && ` (${daysUntil} day${daysUntil !== 1 ? 's' : ''})`}
                      </span>
                    </div>
                  </div>

                  <div className="subscription-actions">
                    <Button
                      variant="secondary"
                      size="small"
                      onClick={() => handleIgnoreClick(subscription)}
                    >
                      Ignore
                    </Button>
                  </div>
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