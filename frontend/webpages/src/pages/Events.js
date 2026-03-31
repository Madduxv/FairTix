import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import '../styles/Events.css';

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
    <div className="events-page">
      <h2>Events</h2>
      {events.length === 0 ? (
        <div className="events-empty">
          <p>No events available.</p>
        </div>
      ) : (
        <div className="events-grid">
          {events.map((event) => (
            <Link key={event.id} to={`/events/${event.id}`} className="event-card">
              <h3>{event.title}</h3>
              <div className="event-card-meta">
                <span>{event.venue}</span>
                <span>{new Date(event.startTime).toLocaleString()}</span>
              </div>
              <div className="event-card-action">View details &rarr;</div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

export default Events;
