import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import TicketCard from '../components/TicketCard';
import '../styles/MyTickets.css';

function MyTickets() {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
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

  if (loading) return (
    <div className="my-tickets">
      <h2>My Tickets</h2>
      <div className="tickets-grid">
        {[1, 2, 3].map((i) => (
          <div key={i} className="ticket-card-skeleton">
            <div className="skeleton-line" style={{ height: '1.25rem', width: '60%', marginBottom: '0.75rem' }} />
            <div className="skeleton-line" style={{ height: '0.9rem', width: '40%', marginBottom: '0.5rem' }} />
            <div className="skeleton-line" style={{ height: '0.9rem', width: '50%', marginBottom: '0.5rem' }} />
            <div className="skeleton-line" style={{ height: '0.9rem', width: '70%' }} />
          </div>
        ))}
      </div>
    </div>
  );

  if (error) return <div className="error-message">{error}</div>;

  return (
    <div className="my-tickets">
      <h2>My Tickets</h2>
      {tickets.length === 0 ? (
        <div className="tickets-empty">
          <p>You don't have any tickets yet.</p>
          <p>Browse events and purchase tickets to see them here.</p>
          <Link to="/events" className="tickets-browse-link">Browse Events</Link>
        </div>
      ) : (
        <div className="tickets-grid">
          {tickets.map((ticket) => (
            <TicketCard key={ticket.id} ticket={ticket} />
          ))}
        </div>
      )}
    </div>
  );
}

export default MyTickets;
