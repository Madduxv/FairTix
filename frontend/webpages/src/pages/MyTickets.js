import React, { useState, useEffect } from 'react';
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

  if (loading) return <div className="loading">Loading tickets...</div>;
  if (error) return <div className="error-message">{error}</div>;

  return (
    <div className="my-tickets">
      <h2>My Tickets</h2>
      {tickets.length === 0 ? (
        <div className="tickets-empty">
          <p>You don't have any tickets yet.</p>
          <p>Browse events and purchase tickets to see them here.</p>
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
