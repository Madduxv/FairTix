import React, { useState } from 'react';
import api from '../api/client';

function TransferDialog({ ticket, onClose, onSuccess }) {
  const [toEmail, setToEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    api.post(`/api/tickets/${ticket.id}/transfer`, { toEmail })
      .then(() => {
        onSuccess();
        onClose();
      })
      .catch((err) => {
        setError(err.message || 'Transfer request failed');
      })
      .finally(() => setSubmitting(false));
  }

  return (
    <div className="dialog-overlay" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <h3>Transfer Ticket</h3>
        <div className="dialog-ticket-summary">
          <p><strong>{ticket.eventTitle}</strong></p>
          <p>Section {ticket.seatSection} · Row {ticket.seatRow} · Seat {ticket.seatNumber}</p>
        </div>
        <form onSubmit={handleSubmit}>
          <label htmlFor="transfer-email">Recipient email</label>
          <input
            id="transfer-email"
            type="email"
            value={toEmail}
            onChange={(e) => setToEmail(e.target.value)}
            placeholder="Enter recipient's email"
            required
            autoFocus
          />
          {error && <p className="error-message">{error}</p>}
          <div className="dialog-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? 'Sending…' : 'Send Transfer Request'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default TransferDialog;
