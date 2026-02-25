import React from 'react';
import { useNavigate } from 'react-router-dom';
import Button from './Button';
import Badge from './Badge';
import './FraudAlertModal.css';

const FraudAlertModal = ({ isOpen, onClose, fraudData }) => {
  const navigate = useNavigate();

  if (!isOpen || !fraudData) return null;

  const handleViewAlerts = () => {
    onClose();
    navigate('/fraud-alerts');
  };

  const getRiskVariant = (riskLevel) => {
    const variants = {
      LOW: 'low',
      MEDIUM: 'medium',
      HIGH: 'high',
      CRITICAL: 'danger'
    };
    return variants[riskLevel] || 'default';
  };

  const getRiskIcon = (riskLevel) => {
    const icons = {
      LOW: '⚠️',
      MEDIUM: '⚠️',
      HIGH: '🚨',
      CRITICAL: '🔴'
    };
    return icons[riskLevel] || '⚠️';
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content fraud-alert-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title-row">
            <span className="fraud-icon">{getRiskIcon(fraudData.riskLevel)}</span>
            <h2>Fraud Alert Detected</h2>
          </div>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>

        <div className="modal-body">
          <div className="fraud-alert-content">
            <div className="fraud-score-section">
              <div className="fraud-score-label">Fraud Score</div>
              <div className="fraud-score-value">{fraudData.fraudScore?.toFixed(0)}/100</div>
              <Badge variant={getRiskVariant(fraudData.riskLevel)} size="large">
                {fraudData.riskLevel} RISK
              </Badge>
            </div>

            <div className="transaction-summary">
              <h3>Transaction Details</h3>
              <div className="summary-row">
                <span className="summary-label">Amount:</span>
                <span className="summary-value amount-highlight">
                  ${fraudData.amount?.toFixed(2)}
                </span>
              </div>
              <div className="summary-row">
                <span className="summary-label">Category:</span>
                <span className="summary-value">{fraudData.category}</span>
              </div>
              {fraudData.location && (
                <div className="summary-row">
                  <span className="summary-label">Location:</span>
                  <span className="summary-value">{fraudData.location}</span>
                </div>
              )}
              {fraudData.description && (
                <div className="summary-row">
                  <span className="summary-label">Description:</span>
                  <span className="summary-value">{fraudData.description}</span>
                </div>
              )}
            </div>

            {fraudData.reasons && fraudData.reasons.length > 0 && (
              <div className="fraud-reasons">
                <h3>Why was this flagged?</h3>
                <ul>
                  {fraudData.reasons.map((reason, index) => (
                    <li key={index}>{reason}</li>
                  ))}
                </ul>
              </div>
            )}

            <div className="fraud-alert-message">
              <p>
                This transaction has been flagged as potentially fraudulent and requires your attention.
                Please review the details and take appropriate action.
              </p>
            </div>
          </div>
        </div>

        <div className="modal-footer">
          <Button variant="secondary" onClick={onClose}>
            Dismiss
          </Button>
          <Button variant="primary" onClick={handleViewAlerts}>
            View All Alerts
          </Button>
        </div>
      </div>
    </div>
  );
};

export default FraudAlertModal;
