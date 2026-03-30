import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../api/client';
import '../styles/EventDetail.css';

function groupSeatsBySection(seats) {
  const groups = {};
  for (const seat of seats) {
    const key = seat.section || 'General';
    if (!groups[key]) groups[key] = [];
    groups[key].push(seat);
  }
  for (const key of Object.keys(groups)) {
    groups[key].sort((a, b) =>
      a.rowLabel.localeCompare(b.rowLabel) || a.seatNumber.localeCompare(b.seatNumber)
    );
  }
  return groups;
}

function buildSummary(seats) {
  const summary = {};
  for (const seat of seats) {
    summary[seat.status] = (summary[seat.status] || 0) + 1;
  }
  return summary;
}

function EventDetail() {
  const { eventId } = useParams();
  const [event, setEvent] = useState(null);
  const [seats, setSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [eventData, seatsData] = await Promise.all([
        api.get(`/api/events/${eventId}`),
        api.get(`/api/events/${eventId}/seats`),
      ]);
      setEvent(eventData);
      setSeats(seatsData || []);
    } catch (err) {
      setError(err.message || 'Failed to load event details.');
    } finally {
      setLoading(false);
    }
  }, [eventId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  if (loading) return <div className="loading">Loading event details...</div>;
  if (error) return (
    <div className="event-detail">
      <Link to="/events" className="event-detail-back">&larr; Back to Events</Link>
      <div className="error-message">{error}</div>
    </div>
  );

  const sectionGroups = groupSeatsBySection(seats);
  const summary = buildSummary(seats);

  return (
    <div className="event-detail">
      <Link to="/events" className="event-detail-back">&larr; Back to Events</Link>

      <div className="event-detail-header">
        <h2>{event.title}</h2>
        <div className="event-detail-meta">
          <span>{event.venue}</span>
          <span>{new Date(event.startTime).toLocaleString()}</span>
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <div className="seat-summary">
          <span className="seat-summary-chip total">{seats.length} total</span>
          {summary.AVAILABLE > 0 && (
            <span className="seat-summary-chip available">{summary.AVAILABLE} available</span>
          )}
          {summary.HELD > 0 && (
            <span className="seat-summary-chip held">{summary.HELD} held</span>
          )}
          {summary.BOOKED > 0 && (
            <span className="seat-summary-chip booked">{summary.BOOKED} booked</span>
          )}
          {summary.SOLD > 0 && (
            <span className="seat-summary-chip sold">{summary.SOLD} sold</span>
          )}
        </div>
        <button className="event-detail-refresh" onClick={fetchData}>Refresh</button>
      </div>

      {seats.length === 0 ? (
        <div className="seats-empty">
          <p>No seats listed for this event yet.</p>
        </div>
      ) : (
        Object.entries(sectionGroups).map(([section, sectionSeats]) => (
          <div key={section} className="seat-section-group">
            <h3>{section}{(() => {
              const prices = sectionSeats.map(s => s.price ?? 0);
              const min = Math.min(...prices);
              const max = Math.max(...prices);
              return min === max
                ? ` — $${min.toFixed(2)}`
                : ` — from $${min.toFixed(2)} to $${max.toFixed(2)}`;
            })()}</h3>
            <table className="seats-table">
              <thead>
                <tr>
                  <th>Row</th>
                  <th>Seat</th>
                  <th>Price</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {sectionSeats.map((seat) => (
                  <tr key={seat.id}>
                    <td>{seat.rowLabel}</td>
                    <td>{seat.seatNumber}</td>
                    <td>${(seat.price ?? 0).toFixed(2)}</td>
                    <td>
                      <span className={`seat-status ${seat.status.toLowerCase()}`}>
                        {seat.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))
      )}
    </div>
  );
}

export default EventDetail;
