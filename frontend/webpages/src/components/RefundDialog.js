import React, { useState } from 'react';
import api from '../api/client';
import '../styles/RefundDialog.css';

const REFUND_WINDOW_DAYS = 14;
const REFUND_REVIEW_DAYS = '3–5 business';

function RefundDialog({ ticket, onClose, onSuccess }) {
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!reason.trim()) {
      setError('Please provide a reason for your refund request.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await api.post(`/api/orders/${ticket.orderId}/refunds`, { reason: reason.trim() });
      onSuccess();
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to submit refund request.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="refund-dialog-overlay" onClick={onClose}>
      <div className="refund-dialog" role="dialog" aria-modal="true" aria-labelledby="refund-dialog-title" onClick={(e) => e.stopPropagation()}>
        <h3 id="refund-dialog-title">Request a Refund</h3>
        <div className="refund-dialog-event">
          <strong>{ticket.eventTitle}</strong>
          <span>
            {ticket.seatSection} / Row {ticket.seatRow} / Seat {ticket.seatNumber}
          </span>
          {ticket.price != null && (
            <span className="refund-amount">Refund amount: ${Number(ticket.price).toFixed(2)}</span>
          )}
        </div>
        <p className="refund-dialog-note">
          Refunds are reviewed within {REFUND_REVIEW_DAYS} days. You must request within {REFUND_WINDOW_DAYS} days of purchase.
          Tickets with used status are not eligible.
        </p>
        <form onSubmit={handleSubmit}>
          <label htmlFor="refund-reason">Reason for refund</label>
          <textarea
            id="refund-reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={4}
            maxLength={1000}
            placeholder="Describe why you are requesting a refund..."
            disabled={loading}
          />
          {error && <p className="refund-dialog-error">{error}</p>}
          <div className="refund-dialog-actions">
            <button type="button" className="btn-cancel-dialog" onClick={onClose} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn-submit-refund" disabled={loading || !reason.trim()}>
              {loading ? 'Submitting...' : 'Submit Request'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default RefundDialog;
