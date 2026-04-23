import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import '../styles/MySupportTickets.css';

const CATEGORIES = [
  { value: 'ORDER_ISSUE', label: 'Order Issue' },
  { value: 'REFUND', label: 'Refund' },
  { value: 'ACCOUNT', label: 'Account' },
  { value: 'EVENT', label: 'Event' },
  { value: 'TECHNICAL', label: 'Technical Problem' },
  { value: 'OTHER', label: 'Other' },
];

function SupportPage() {
  useEffect(() => { document.title = 'Submit Support Ticket | FairTix'; }, []);
  const [subject, setSubject] = useState('');
  const [category, setCategory] = useState('ORDER_ISSUE');
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    if (!subject.trim() || !message.trim()) {
      setError('Subject and message are required.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const ticket = await api.post('/api/support/tickets', { subject, category, message });
      navigate(`/support/tickets/${ticket.id}`);
    } catch (err) {
      setError(err.message || 'Failed to submit ticket. Please try again.');
      setSubmitting(false);
    }
  }

  return (
    <div className="support-new">
      <h2>Open a Support Ticket</h2>
      <p className="support-intro">
        Describe your issue and our team will respond as soon as possible.
      </p>
      <form className="support-form" onSubmit={handleSubmit}>
        <label className="support-label">
          Subject
          <input
            className="support-input"
            type="text"
            maxLength={200}
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            placeholder="Brief description of the issue"
            required
          />
        </label>

        <label className="support-label">
          Category
          <select
            className="support-select"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
          >
            {CATEGORIES.map((c) => (
              <option key={c.value} value={c.value}>{c.label}</option>
            ))}
          </select>
        </label>

        <label className="support-label">
          Message
          <textarea
            className="support-textarea"
            rows={6}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            placeholder="Please provide as much detail as possible..."
            required
          />
        </label>

        {error && <p className="error-message">{error}</p>}

        <div className="support-form-actions">
          <button
            type="button"
            className="btn-secondary"
            onClick={() => navigate('/support')}
            disabled={submitting}
          >
            Cancel
          </button>
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'Submitting…' : 'Submit Ticket'}
          </button>
        </div>
      </form>
    </div>
  );
}

export default SupportPage;
