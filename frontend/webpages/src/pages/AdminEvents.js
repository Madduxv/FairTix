import { useState, useEffect } from 'react';
import api from '../api/client';

function AdminEvents() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [title, setTitle] = useState('');
  const [venue, setVenue] = useState('');
  const [startTime, setStartTime] = useState('');
  const [creating, setCreating] = useState(false);

  function loadEvents() {
    setLoading(true);
    api.get('/api/events')
      .then((data) => {
        setEvents(data.content || []);
        setError('');
      })
      .catch((err) => setError(err.message || 'Failed to load events'))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    loadEvents();
  }, []);

  async function handleCreate(e) {
    e.preventDefault();
    setCreating(true);
    setError('');
    try {
      await api.post('/api/events', {
        title,
        venue,
        startTime: new Date(startTime).toISOString(),
      });
      setTitle('');
      setVenue('');
      setStartTime('');
      loadEvents();
    } catch (err) {
      setError(err.message || 'Failed to create event');
    } finally {
      setCreating(false);
    }
  }

  async function handleDelete(id) {
    try {
      await api.delete(`/api/events/${id}`);
      loadEvents();
    } catch (err) {
      setError(err.message || 'Failed to delete event');
    }
  }

  return (
    <div style={{ padding: '2rem' }}>
      <h2>Manage Events</h2>

      <form onSubmit={handleCreate} style={{ marginBottom: '2rem' }}>
        <h3>Create Event</h3>
        {error && <div className="error-message">{error}</div>}
        <div style={{ marginBottom: '0.5rem' }}>
          <input
            type="text"
            placeholder="Event title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
        </div>
        <div style={{ marginBottom: '0.5rem' }}>
          <input
            type="text"
            placeholder="Venue"
            value={venue}
            onChange={(e) => setVenue(e.target.value)}
            required
          />
        </div>
        <div style={{ marginBottom: '0.5rem' }}>
          <input
            type="datetime-local"
            value={startTime}
            onChange={(e) => setStartTime(e.target.value)}
            required
          />
        </div>
        <button type="submit" disabled={creating}>
          {creating ? 'Creating...' : 'Create Event'}
        </button>
      </form>

      <h3>All Events</h3>
      {loading ? (
        <p>Loading...</p>
      ) : events.length === 0 ? (
        <p>No events yet.</p>
      ) : (
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {events.map((event) => (
            <li key={event.id} style={{ padding: '1rem', borderBottom: '1px solid #eee', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <strong>{event.title}</strong>
                <p>{event.venue} — {new Date(event.startTime).toLocaleString()}</p>
              </div>
              <button onClick={() => handleDelete(event.id)} style={{ color: 'red' }}>
                Delete
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default AdminEvents;
