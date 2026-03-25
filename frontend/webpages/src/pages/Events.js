import React, { useState, useEffect } from 'react';
import api from '../api/client';

function Events() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/api/events')
      .then((data) => {
        setEvents(data.content || []);
      })
      .catch((err) => {
        setError(err.message || 'Failed to load events');
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  if (loading) return <div className="loading">Loading events...</div>;
  if (error) return <div className="error-message">{error}</div>;

  return (
    <div style={{ padding: '2rem' }}>
      <h2>Events</h2>
      {events.length === 0 ? (
        <p>No events available.</p>
      ) : (
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {events.map((event) => (
            <li key={event.id} style={{ padding: '1rem', borderBottom: '1px solid #eee' }}>
              <h3>{event.title}</h3>
              <p>{event.venue} — {new Date(event.startTime).toLocaleString()}</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default Events;
