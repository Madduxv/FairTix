import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import api from '../api/client';
import '../styles/EventDetail.css';

const MAX_SEATS_PER_HOLD = 10;

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
  const { user } = useAuth();
  const [event, setEvent] = useState(null);
  const [seats, setSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedSeatIds, setSelectedSeatIds] = useState(new Set());
  const [selectionError, setSelectionError] = useState('');

  function toggleSeat(seatId) {
    setSelectionError('');
    setSelectedSeatIds((prev) => {
      const next = new Set(prev);
      if (next.has(seatId)) {
        next.delete(seatId);
      } else {
        if (next.size >= MAX_SEATS_PER_HOLD) {
          setSelectionError(`You can select up to ${MAX_SEATS_PER_HOLD} seats per hold.`);
          return prev;
        }
        next.add(seatId);
      }
      return next;
    });
  }

  function clearSelection() {
    setSelectedSeatIds(new Set());
    setSelectionError('');
  }

  const [holdSubmitting, setHoldSubmitting] = useState(false);
  const [holdMessage, setHoldMessage] = useState(null); // { type: 'success'|'error', text }

  async function handleHoldSeats() {
    if (selectedSeatIds.size === 0 || holdSubmitting) return;
    setHoldSubmitting(true);
    setHoldMessage(null);
    setSelectionError('');
    try {
      await api.post(`/api/events/${eventId}/holds`, {
        seatIds: [...selectedSeatIds],
      });
      setSelectedSeatIds(new Set());
      setHoldMessage({ type: 'success', text: 'Hold created — expires in 10 minutes.' });
      await fetchData();
    } catch (err) {
      if (err.status === 409) {
        setHoldMessage({ type: 'error', text: 'Some seats are no longer available.' });
      } else if (err.status === 401 || err.status === 403) {
        setHoldMessage({ type: 'error', text: 'Session expired — please log in again.' });
      } else {
        setHoldMessage({ type: 'error', text: err.message || 'Failed to create hold.' });
      }
      setSelectedSeatIds(new Set());
      await fetchData();
    } finally {
      setHoldSubmitting(false);
    }
  }

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
            <h3>{section}</h3>
            <table className="seats-table">
              <thead>
                <tr>
                  <th>Row</th>
                  <th>Seat</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {sectionSeats.map((seat) => {
                  const isAvailable = seat.status === 'AVAILABLE';
                  const isSelected = selectedSeatIds.has(seat.id);
                  const canSelect = user && isAvailable;
                  return (
                    <tr
                      key={seat.id}
                      className={[
                        canSelect ? 'seat-row-selectable' : '',
                        isSelected ? 'seat-row-selected' : '',
                      ].join(' ')}
                      onClick={canSelect ? () => toggleSeat(seat.id) : undefined}
                      role={canSelect ? 'button' : undefined}
                      tabIndex={canSelect ? 0 : -1}
                      onKeyDown={
                        canSelect
                          ? (e) => {
                              if (e.key === 'Enter' || e.key === ' ') {
                                e.preventDefault();
                                toggleSeat(seat.id);
                              }
                            }
                          : undefined
                      }
                    >
                      <td>{seat.rowLabel}</td>
                      <td>{seat.seatNumber}</td>
                      <td>
                        <span className={`seat-status ${seat.status.toLowerCase()}`}>
                          {isSelected ? 'SELECTED' : seat.status}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ))
      )}

      {!user && seats.length > 0 && (
        <div className="login-prompt">
          <Link to="/login">Log in</Link> to hold seats.
        </div>
      )}

      {holdMessage && (
        <div className={`hold-message ${holdMessage.type}`}>
          {holdMessage.text}
          {holdMessage.type === 'success' && (
            <> <Link to="/my-holds" className="hold-message-link">View My Holds</Link></>
          )}
        </div>
      )}

      {selectionError && (
        <div className="selection-error">{selectionError}</div>
      )}

      {selectedSeatIds.size > 0 && (
        <div className="selection-bar">
          <span>{selectedSeatIds.size} seat{selectedSeatIds.size > 1 ? 's' : ''} selected</span>
          <div className="selection-bar-actions">
            <button className="selection-bar-clear" onClick={clearSelection} disabled={holdSubmitting}>Clear</button>
            <button className="selection-bar-hold" onClick={handleHoldSeats} disabled={holdSubmitting}>
              {holdSubmitting ? 'Holding...' : 'Hold Seats'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default EventDetail;
