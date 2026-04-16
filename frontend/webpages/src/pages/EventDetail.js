import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import api from '../api/client';
import WaitingRoom from '../components/WaitingRoom';
import '../styles/EventDetail.css';

const POLL_INTERVAL_MS = 10000;
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
  const [holdDuration, setHoldDuration] = useState(10);
  const [ownedTicketCount, setOwnedTicketCount] = useState(0);

  const prevSeatsRef = useRef([]);

  // Queue state
  const [queueStatus, setQueueStatus] = useState(null); // null | 'WAITING' | 'ADMITTED' | 'EXPIRED'
  const [joiningQueue, setJoiningQueue] = useState(false);
  const [queueError, setQueueError] = useState('');
  const [admissionExpiresAt, setAdmissionExpiresAt] = useState(null);
  const [admissionCountdown, setAdmissionCountdown] = useState('');

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
        const cap = event && event.maxTicketsPerUser;
        if (cap != null && ownedTicketCount + next.size + 1 > cap) {
          setSelectionError(`You have reached the purchase limit of ${cap} ticket(s) for this event.`);
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

  const navigate = useNavigate();
  const [holdSubmitting, setHoldSubmitting] = useState(false);
  const [holdMessage, setHoldMessage] = useState(null);
  const [createdHoldIds, setCreatedHoldIds] = useState([]);
  const [confirmingCheckout, setConfirmingCheckout] = useState(false);

  async function handleHoldSeats() {
    if (selectedSeatIds.size === 0 || holdSubmitting) return;
    setHoldSubmitting(true);
    setHoldMessage(null);
    setSelectionError('');
    try {
      const holds = await api.post(`/api/events/${eventId}/holds`, {
        seatIds: [...selectedSeatIds],
        durationMinutes: holdDuration,
      });
      const holdIds = (holds || []).map((h) => h.id);
      setCreatedHoldIds(holdIds);
      setSelectedSeatIds(new Set());
      setHoldMessage({
        type: 'success',
        text: `Hold created \u2014 expires in ${holdDuration} minutes.`,
      });
      await fetchData();
    } catch (err) {
      if (err.status === 409) {
        const detail = err.message || 'Some seats are no longer available.';
        setHoldMessage({ type: 'error', text: detail });
      } else if (err.status === 401 || err.status === 403) {
        setHoldMessage({ type: 'error', text: 'Session expired \u2014 please log in again.' });
      } else {
        setHoldMessage({ type: 'error', text: err.message || 'Failed to create hold.' });
      }
      setCreatedHoldIds([]);
      setSelectedSeatIds(new Set());
      await fetchData();
    } finally {
      setHoldSubmitting(false);
    }
  }

  async function handleConfirmAndCheckout() {
    if (createdHoldIds.length === 0 || confirmingCheckout) return;
    setConfirmingCheckout(true);
    setHoldMessage(null);
    try {
      await Promise.all(
        createdHoldIds.map((id) => api.post(`/api/holds/${id}/confirm`))
      );
      navigate('/checkout', { state: { holdIds: createdHoldIds } });
    } catch (err) {
      setHoldMessage({
        type: 'error',
        text: err.message || 'Failed to confirm holds. Please try from My Holds.',
      });
    } finally {
      setConfirmingCheckout(false);
    }
  }

  const fetchData = useCallback(async () => {
    setError('');
    try {
      const [eventData, seatsData] = await Promise.all([
        api.get(`/api/events/${eventId}`),
        api.get(`/api/events/${eventId}/seats`),
      ]);
      setEvent(eventData);
      if (user && eventData.maxTicketsPerUser != null) {
        try {
          const myTickets = await api.get('/api/tickets');
          const count = (myTickets || []).filter(
            (t) => String(t.eventId).toLowerCase() === String(eventId).toLowerCase() && t.status !== 'CANCELLED'
          ).length;
          setOwnedTicketCount(count);
        } catch (_) {
          // non-critical
        }
      }
      // Reset queue state if event doesn't require queue
      if (!eventData.queueRequired) {
        setQueueStatus(null);
        setQueueError('');
        setAdmissionExpiresAt(null);
      }
      // Fetch queue status if event requires queue and user is logged in
      if (eventData.queueRequired && user) {
        try {
          const qs = await api.get(`/api/events/${eventId}/queue/status`);
          setQueueStatus(qs.status);
          if (qs.status === 'ADMITTED') {
            setAdmissionExpiresAt(qs.expiresAt);
          }
        } catch (qErr) {
          // 404 means user hasn't joined yet
          if (qErr.status !== 404) {
            setQueueError(qErr.message || 'Failed to check queue status.');
          } else {
            setQueueStatus(null);
          }
        }
      }
      const newSeats = seatsData || [];

      if (prevSeatsRef.current.length > 0) {
        // Deselect seats that are no longer available
        setSelectedSeatIds((prev) => {
          const next = new Set(prev);
          let changed = false;
          for (const seat of newSeats) {
            if (next.has(seat.id) && seat.status !== 'AVAILABLE') {
              next.delete(seat.id);
              changed = true;
            }
          }
          return changed ? next : prev;
        });
      }

      prevSeatsRef.current = newSeats;
      setSeats(newSeats);
    } catch (err) {
      setError(err.message || 'Failed to load event details.');
    } finally {
      setLoading(false);
    }
  }, [eventId, user]);

  useEffect(() => {
    setLoading(true);
    setQueueStatus(null);
    setQueueError('');
    setAdmissionExpiresAt(null);
    setAdmissionCountdown('');
    fetchData();
  }, [fetchData]);

  // Auto-poll seat availability every 10 seconds (only when tab is visible)
  useEffect(() => {
    const interval = setInterval(() => {
      if (!document.hidden) {
        fetchData();
      }
    }, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [fetchData]);

  // Countdown timer for admission window
  useEffect(() => {
    if (!admissionExpiresAt) return;
    const tick = () => {
      const diff = Math.max(0, new Date(admissionExpiresAt) - Date.now());
      const mins = Math.floor(diff / 60000);
      const secs = Math.floor((diff % 60000) / 1000);
      setAdmissionCountdown(`${mins}:${secs.toString().padStart(2, '0')}`);
      if (diff === 0) setQueueStatus('EXPIRED');
    };
    tick();
    const interval = setInterval(tick, 1000);
    return () => clearInterval(interval);
  }, [admissionExpiresAt]);

  async function handleJoinQueue() {
    if (joiningQueue) return;
    setJoiningQueue(true);
    setQueueError('');
    try {
      await api.post(`/api/events/${eventId}/queue/join`);
      setQueueStatus('WAITING');
    } catch (err) {
      setQueueError(err.message || 'Failed to join queue.');
    } finally {
      setJoiningQueue(false);
    }
  }

  if (loading) return (
    <div className="event-detail">
      <Link to="/events" className="event-detail-back">&larr; Back to Events</Link>
      <div className="event-detail-skeleton">
        <div className="skeleton-line" style={{ height: '1.5rem', width: '60%', marginBottom: '0.75rem' }} />
        <div className="skeleton-line" style={{ height: '1rem', width: '40%', marginBottom: '1.5rem' }} />
        <div className="skeleton-line" style={{ height: '0.9rem', width: '30%', marginBottom: '0.5rem' }} />
        {[1, 2, 3, 4, 5].map((i) => (
          <div key={i} className="skeleton-line" style={{ height: '2rem', width: '100%', marginBottom: '0.4rem' }} />
        ))}
      </div>
    </div>
  );
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
          <span>{event.venue?.name ?? ''}</span>
          <span>{new Date(event.startTime).toLocaleString()}</span>
        </div>
      </div>

      {/* Queue section for queue-required events */}
      {event.queueRequired && user && queueStatus === null && (
        <div className="queue-join-section">
          {event.maxTicketsPerUser != null && ownedTicketCount >= event.maxTicketsPerUser ? (
            <p className="queue-join-message">You have reached the purchase limit for this event and cannot rejoin the queue.</p>
          ) : (
            <>
              <p className="queue-join-message">This event requires queue admission before you can hold seats.</p>
              {queueError && <div className="queue-error">{queueError}</div>}
              <button className="queue-join-btn" onClick={handleJoinQueue} disabled={joiningQueue}>
                {joiningQueue ? 'Joining...' : 'Join Queue'}
              </button>
            </>
          )}
        </div>
      )}

      {event.queueRequired && user && queueStatus === 'WAITING' && (
        <WaitingRoom
          eventId={eventId}
          onAdmitted={(expiresAt) => { setQueueStatus('ADMITTED'); setAdmissionExpiresAt(expiresAt); }}
          onLeft={() => setQueueStatus(null)}
        />
      )}

      {event.queueRequired && user && queueStatus === 'ADMITTED' && (
        <div className="queue-admitted-banner">
          {event.maxTicketsPerUser != null && ownedTicketCount >= event.maxTicketsPerUser ? (
            'You have already purchased the maximum number of tickets for this event.'
          ) : summary.AVAILABLE > 0 ? (
            <>
              You're admitted! Select your seats before your window closes.
              {admissionCountdown && <span className="queue-countdown"> Time remaining: {admissionCountdown}</span>}
            </>
          ) : (
            'All seats are sold out. No seats are available to select during your admission window.'
          )}
        </div>
      )}

      {event.queueRequired && user && queueStatus === 'EXPIRED' && (
        <div className="queue-join-section">
          {event.maxTicketsPerUser != null && ownedTicketCount >= event.maxTicketsPerUser ? (
            <p className="queue-join-message">You have reached the purchase limit for this event and cannot rejoin the queue.</p>
          ) : (
            <>
              <p className="queue-join-message">Your admission window expired. You can rejoin the queue.</p>
              {queueError && <div className="queue-error">{queueError}</div>}
              <button className="queue-join-btn" onClick={handleJoinQueue} disabled={joiningQueue}>
                {joiningQueue ? 'Rejoining...' : 'Rejoin Queue'}
              </button>
            </>
          )}
        </div>
      )}

      {event.queueRequired && user && queueStatus === 'COMPLETED' && (
        <div className="queue-admitted-banner">
          You've already reserved a seat for this event.{' '}
          <Link to="/my-holds">Complete your purchase in My Holds.</Link>
          {(event.maxTicketsPerUser == null || ownedTicketCount < event.maxTicketsPerUser) && (
            <span style={{ marginLeft: '1rem', fontSize: '0.85em' }}>
              Released your hold?{' '}
              <button
                className="queue-join-btn"
                style={{ display: 'inline', padding: '0.2rem 0.6rem', fontSize: '0.85em' }}
                onClick={async () => {
                  setQueueStatus(null);
                  await handleJoinQueue();
                }}
                disabled={joiningQueue}
              >
                {joiningQueue ? 'Rejoining...' : 'Rejoin Queue'}
              </button>
            </span>
          )}
        </div>
      )}

      {event.queueRequired && !user && (
        <div className="login-prompt">
          <Link to="/login">Log in</Link> to join the queue for this event.
        </div>
      )}

      {user && event.maxTicketsPerUser != null && (
        <div className="purchase-cap-notice">
          Purchase limit: {ownedTicketCount} / {event.maxTicketsPerUser} ticket(s) used for this event.
        </div>
      )}

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

      {event.queueRequired && user && queueStatus === 'WAITING' ? null : seats.length === 0 ? (
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
                ? ` \u2014 $${min.toFixed(2)}`
                : ` \u2014 from $${min.toFixed(2)} to $${max.toFixed(2)}`;
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
                {sectionSeats.map((seat) => {
                  const isAvailable = seat.status === 'AVAILABLE';
                  const isSelected = selectedSeatIds.has(seat.id);
                  const queueGated = event.queueRequired && queueStatus !== 'ADMITTED';
                  const atCap = event.maxTicketsPerUser != null && ownedTicketCount >= event.maxTicketsPerUser;
                  const canSelect = user && isAvailable && !queueGated && !atCap;
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
                      <td>${seat.price != null ? Number(seat.price).toFixed(2) : '—'}</td>
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

      {seats.length > 0 && (!summary.AVAILABLE || summary.AVAILABLE === 0) && (
        <div className="seats-all-taken">
          <p>All seats are currently held or sold. Check back later or refresh to see updates.</p>
        </div>
      )}

      {!user && seats.length > 0 && (
        <div className="login-prompt">
          <Link to="/login">Log in</Link> to hold seats.
        </div>
      )}

      {holdMessage && (
        <div className={`hold-message ${holdMessage.type}`}>
          {holdMessage.text}
          {holdMessage.type === 'success' && createdHoldIds.length > 0 && (
            <div className="hold-message-actions">
              <button
                className="hold-message-checkout"
                onClick={handleConfirmAndCheckout}
                disabled={confirmingCheckout}
              >
                {confirmingCheckout ? 'Confirming...' : 'Confirm & Checkout'}
              </button>
              <Link to="/my-holds" className="hold-message-link">View My Holds</Link>
            </div>
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
            <label className="hold-duration-label">
              Hold for:
              <select
                value={holdDuration}
                onChange={(e) => setHoldDuration(Number(e.target.value))}
                className="hold-duration-select"
              >
                <option value={5}>5 min</option>
                <option value={10}>10 min</option>
                <option value={15}>15 min</option>
                <option value={30}>30 min</option>
                <option value={60}>60 min</option>
              </select>
            </label>
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
