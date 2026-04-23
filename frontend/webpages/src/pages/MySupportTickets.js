import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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

function MySupportTickets() {
  useEffect(() => { document.title = 'My Support Tickets | FairTix'; }, []);
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const navigate = useNavigate();

  const fetchTickets = useCallback((p = 0) => {
    setLoading(true);
    setError('');
    api.get(`/api/support/tickets?page=${p}`)
      .then((data) => {
        setTickets(data?.content || []);
        setTotalPages(data?.page?.totalPages ?? 1);
        setPage(p);
      })
      .catch((err) => setError(err.message || 'Failed to load support tickets'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    fetchTickets();
  }, [fetchTickets]);

  return (
    <div className="support-tickets">
      <div className="support-tickets-header">
        <h2>My Support Tickets</h2>
        <button className="btn-primary" onClick={() => navigate('/support/new')}>
          New Ticket
        </button>
      </div>

      {loading && (
        <div className="support-tickets-list">
          {[1, 2, 3].map((i) => (
            <div key={i} className="support-ticket-row support-ticket-skeleton">
              <div className="skeleton-line skeleton-subject" />
              <div className="skeleton-line skeleton-meta" />
            </div>
          ))}
        </div>
      )}

      {!loading && error && (
        <div className="support-error">
          <p className="error-message">{error}</p>
          <button className="tickets-retry" onClick={() => fetchTickets(page)}>Retry</button>
        </div>
      )}

      {!loading && !error && tickets.length === 0 && (
        <div className="support-empty">
          <p>You have no support tickets.</p>
          <p>Need help? <Link to="/support/new">Open a new ticket</Link>.</p>
        </div>
      )}

      {!loading && !error && tickets.length > 0 && (
        <>
          <div className="support-tickets-list">
            {tickets.map((ticket) => (
              <Link
                key={ticket.id}
                to={`/support/tickets/${ticket.id}`}
                className="support-ticket-row"
              >
                <div className="support-ticket-subject">{ticket.subject}</div>
                <div className="support-ticket-meta">
                  <span className={`support-status-badge ${STATUS_CLASS[ticket.status] || ''}`}>
                    {STATUS_LABELS[ticket.status] || ticket.status}
                  </span>
                  <span className="support-ticket-category">{ticket.category.replace('_', ' ')}</span>
                  <span className="support-ticket-date">
                    {new Date(ticket.updatedAt).toLocaleDateString()}
                  </span>
                </div>
              </Link>
            ))}
          </div>
          {totalPages > 1 && (
            <div className="support-pagination">
              <button disabled={page === 0} onClick={() => fetchTickets(page - 1)}>Previous</button>
              <span>Page {page + 1} of {totalPages}</span>
              <button disabled={page >= totalPages - 1} onClick={() => fetchTickets(page + 1)}>Next</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default MySupportTickets;
