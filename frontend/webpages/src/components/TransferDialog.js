import React, { useState } from 'react';
import api from '../api/client';
import Modal from './ui/Modal';
import Button from './ui/Button';
import '../styles/TransferDialog.css';

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
    <Modal onClose={onClose} title="Transfer Ticket">
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
          <Button type="button" variant="ghost" size="sm" onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="nav" size="sm" disabled={submitting}>
            {submitting ? 'Sending…' : 'Send Transfer Request'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export default TransferDialog;
