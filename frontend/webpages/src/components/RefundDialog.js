import React, { useState } from 'react';
import api from '../api/client';
import Modal from './ui/Modal';
import Button from './ui/Button';
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
    <Modal onClose={onClose} title="Request a Refund" titleId="refund-dialog-title">
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
          <Button type="button" variant="ghost" size="sm" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" size="sm" disabled={loading || !reason.trim()}>
            {loading ? 'Submitting...' : 'Submit Request'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export default RefundDialog;
