import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import TicketCard from '../components/TicketCard';
import '../styles/MyTickets.css';

function MyTickets() {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchTickets = useCallback(() => {
    setLoading(true);
    setError('');
    api.get('/api/tickets')
      .then((data) => {
        setTickets(data || []);
      })
      .catch((err) => {
        setError(err.message || 'Failed to load tickets');
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    fetchTickets();
  }, [fetchTickets]);

  return (
    <div className="my-tickets">
      <h2>My Tickets</h2>

      {loading && (
        <div className="tickets-grid">
          {[1, 2, 3].map((i) => (
            <div key={i} className="ticket-card ticket-card-skeleton">
              <div className="skeleton-line skeleton-ticket-title" />
              <div className="skeleton-line skeleton-ticket-meta" />
              <div className="skeleton-line skeleton-ticket-seat" />
            </div>
          ))}
        </div>
      )}

      {!loading && error && (
        <div className="tickets-error">
          <p className="error-message">{error}</p>
          <button className="tickets-retry" onClick={fetchTickets}>Retry</button>
        </div>
      )}

      {!loading && !error && tickets.length === 0 && (
        <div className="tickets-empty">
          <p>You don't have any tickets yet.</p>
          <p>Browse events and purchase tickets to see them here.</p>
          <Link to="/events" className="tickets-browse-link">Browse Events</Link>
        </div>
      )}

      {!loading && !error && tickets.length > 0 && (
        <div className="tickets-grid">
          {tickets.map((ticket) => (
            <TicketCard key={ticket.id} ticket={ticket} onTransferred={fetchTickets} />
          ))}
        </div>
      )}
    </div>
  );
}

export default MyTickets;
