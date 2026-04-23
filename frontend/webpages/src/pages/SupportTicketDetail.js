import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import api from '../api/client';
import '../styles/MySupportTickets.css';

const STATUS_LABELS = {
  OPEN: 'Open',
  IN_PROGRESS: 'In Progress',
  WAITING_ON_USER: 'Waiting on You',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
};

const STATUS_CLASS = {
  OPEN: 'status-open',
  IN_PROGRESS: 'status-in-progress',
  WAITING_ON_USER: 'status-waiting',
  RESOLVED: 'status-resolved',
  CLOSED: 'status-closed',
};

function SupportTicketDetail() {
  useEffect(() => { document.title = 'Support Ticket | FairTix'; }, []);
  const { id } = useParams();
  const navigate = useNavigate();
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reply, setReply] = useState('');
  const [sending, setSending] = useState(false);
  const [replyError, setReplyError] = useState('');
  const [closing, setClosing] = useState(false);

  const fetchTicket = useCallback(() => {
    setLoading(true);
    setError('');
    api.get(`/api/support/tickets/${id}`)
      .then((data) => setTicket(data))
      .catch((err) => setError(err.message || 'Failed to load ticket'))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    fetchTicket();
  }, [fetchTicket]);

  async function handleReply(e) {
    e.preventDefault();
    if (!reply.trim()) return;
    setSending(true);
    setReplyError('');
    try {
      await api.post(`/api/support/tickets/${id}/messages`, { message: reply });
      setReply('');
      fetchTicket();
    } catch (err) {
      setReplyError(err.message || 'Failed to send reply');
    } finally {
      setSending(false);
    }
  }

  async function handleClose() {
    if (!window.confirm('Close this ticket?')) return;
    setClosing(true);
    try {
      await api.post(`/api/support/tickets/${id}/close`);
      fetchTicket();
    } catch (err) {
      setError(err.message || 'Failed to close ticket');
    } finally {
      setClosing(false);
    }
  }

  if (loading) return <div className="support-loading">Loading ticket…</div>;
  if (error) return (
    <div className="support-error">
      <p className="error-message">{error}</p>
      <div style={{ display: 'flex', gap: '0.5rem' }}>
        <button onClick={fetchTicket}>Retry</button>
        <button onClick={() => navigate('/support')}>Back to tickets</button>
      </div>
    </div>
  );
  if (!ticket) return null;

  const isClosed = ticket.status === 'CLOSED' || ticket.status === 'RESOLVED';

  return (
    <div className="support-detail">
      <div className="support-detail-header">
        <button className="btn-back" onClick={() => navigate('/support')}>← Back</button>
        <div className="support-detail-title">
          <h2>{ticket.subject}</h2>
          <span className={`support-status-badge ${STATUS_CLASS[ticket.status] || ''}`}>
            {STATUS_LABELS[ticket.status] || ticket.status}
          </span>
        </div>
        <div className="support-detail-meta">
          <span>Category: {ticket.category.replace('_', ' ')}</span>
          <span>Opened: {new Date(ticket.createdAt).toLocaleString()}</span>
        </div>
      </div>

      <div className="support-messages">
        {(!ticket.messages || ticket.messages.length === 0) && (
          <p className="support-no-messages">No messages yet.</p>
        )}
        {(ticket.messages || []).map((msg) => (
          <div
            key={msg.id}
            className={`support-message ${msg.isStaff ? 'support-message-staff' : 'support-message-user'}`}
          >
            <div className="support-message-author">
              {msg.isStaff ? 'Support Team' : msg.authorEmail}
            </div>
            <div className="support-message-body">{msg.message}</div>
            <div className="support-message-time">
              {new Date(msg.createdAt).toLocaleString()}
            </div>
          </div>
        ))}
      </div>

      {!isClosed && (
        <form className="support-reply-form" onSubmit={handleReply}>
          <textarea
            className="support-textarea"
            rows={4}
            value={reply}
            onChange={(e) => setReply(e.target.value)}
            placeholder="Write a reply…"
            required
          />
          {replyError && <p className="error-message">{replyError}</p>}
          <div className="support-reply-actions">
            <button type="submit" className="btn-primary" disabled={sending}>
              {sending ? 'Sending…' : 'Send Reply'}
            </button>
            <button
              type="button"
              className="btn-secondary"
              onClick={handleClose}
              disabled={closing}
            >
              {closing ? 'Closing…' : 'Close Ticket'}
            </button>
          </div>
        </form>
      )}

      {isClosed && (
        <div className="support-closed-notice">
          This ticket is closed. <Link to="/support/new">Open a new ticket</Link> if you need further help.
        </div>
      )}
    </div>
  );
}

export default SupportTicketDetail;
