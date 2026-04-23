import { useState, useEffect, useCallback, useRef } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import '../styles/MyHolds.css';

function formatTimeLeft(expiresAt) {
  const diff = new Date(expiresAt).getTime() - Date.now();
  if (diff <= 0) return 'Expired';
  const mins = Math.floor(diff / 60000);
  const secs = Math.floor((diff % 60000) / 1000);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

function MyHolds() {
  useEffect(() => { document.title = 'My Holds | FairTix'; }, []);
  const [holds, setHolds] = useState([]);
  const [seatMap, setSeatMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState({});
  const [message, setMessage] = useState(null);
  const [, setTick] = useState(0);
  const tickRef = useRef(null);

  const fetchHolds = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [activeData, confirmedData] = await Promise.all([
        api.get('/api/holds?status=ACTIVE'),
        api.get('/api/holds?status=CONFIRMED'),
      ]);
      const data = [...(activeData || []), ...(confirmedData || [])];
      setHolds(data);

      // Collect unique event IDs to fetch seat details
      const eventIds = [...new Set((data || []).map((h) => h.eventId))];
      const seatEntries = {};
      await Promise.all(
        eventIds.map(async (eid) => {
          const seats = await api.get(`/api/events/${eid}/seats`);
          for (const s of seats) {
            seatEntries[s.id] = s;
          }
        })
      );
      setSeatMap(seatEntries);
    } catch (err) {
      setError(err.message || 'Failed to load holds.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchHolds();
  }, [fetchHolds]);

  // Countdown ticker — update every second while there are active holds
  useEffect(() => {
    const hasActive = holds.some(
      (h) => h.status === 'ACTIVE' && new Date(h.expiresAt).getTime() > Date.now()
    );
    if (hasActive) {
      tickRef.current = setInterval(() => setTick((t) => t + 1), 1000);
    }
    return () => clearInterval(tickRef.current);
  }, [holds]);

  async function handleConfirm(holdId) {
    setActionLoading((prev) => ({ ...prev, [holdId]: 'confirm' }));
    setMessage(null);
    try {
      const updated = await api.post(`/api/holds/${holdId}/confirm`);
      setHolds((prev) => prev.map((h) => (h.id === holdId ? updated : h)));
      setMessage({ type: 'success', text: 'Hold confirmed.' });
    } catch (err) {
      setMessage({ type: 'error', text: err.message || 'Failed to confirm hold.' });
    } finally {
      setActionLoading((prev) => ({ ...prev, [holdId]: null }));
    }
  }

  async function handleRelease(holdId) {
    setActionLoading((prev) => ({ ...prev, [holdId]: 'release' }));
    setMessage(null);
    try {
      await api.post(`/api/holds/${holdId}/release`);
      setHolds((prev) => prev.filter((h) => h.id !== holdId));
      setMessage({ type: 'success', text: 'Hold released.' });
    } catch (err) {
      setMessage({ type: 'error', text: err.message || 'Failed to release hold.' });
    } finally {
      setActionLoading((prev) => ({ ...prev, [holdId]: null }));
    }
  }

  const activeHolds = holds.filter((h) => h.status === 'ACTIVE');
  const confirmedHolds = holds.filter((h) => h.status === 'CONFIRMED');

  return (
    <div className="my-holds">
      <div className="my-holds-header">
        <h2>My Holds</h2>
        <button className="my-holds-refresh" onClick={fetchHolds} disabled={loading}>Refresh</button>
      </div>

      {message && (
        <div className={`hold-page-message ${message.type}`}>{message.text}</div>
      )}

      {loading && (
        <div className="holds-grid">
          {[1, 2, 3].map((i) => (
            <div key={i} className="hold-card hold-card-skeleton">
              <div className="skeleton-line skeleton-hold-seat" />
              <div className="skeleton-line skeleton-hold-timer" />
              <div className="skeleton-line skeleton-hold-actions" />
            </div>
          ))}
        </div>
      )}

      {!loading && error && (
        <div className="holds-error">
          <p className="error-message">{error}</p>
          <button className="my-holds-refresh" onClick={fetchHolds}>Retry</button>
        </div>
      )}

      {!loading && !error && holds.length === 0 && (
        <div className="holds-empty">
          <p>No active holds.</p>
          <p>Browse <Link to="/events">events</Link> and select seats to hold them.</p>
        </div>
      )}

      {!loading && !error && holds.length > 0 && (
        <>
          {activeHolds.length > 0 && (
            <section>
              <h3 className="holds-section-title">Active</h3>
              <div className="holds-grid">
                {activeHolds.map((hold) => {
                  const seat = seatMap[hold.seatId];
                  const expired = new Date(hold.expiresAt).getTime() <= Date.now();
                  return (
                    <div key={hold.id} className={`hold-card ${expired ? 'hold-card-expired' : ''}`}>
                      <div className="hold-card-header">
                        <span className="hold-card-seat">
                          {seat
                            ? `${seat.section} — Row ${seat.rowLabel}, Seat ${seat.seatNumber}`
                            : `Seat ${hold.seatId.slice(0, 8)}...`}
                        </span>
                        <span className={`hold-card-timer ${expired ? 'expired' : ''}`}>
                          {formatTimeLeft(hold.expiresAt)}
                        </span>
                      </div>
                      <div className="hold-card-actions">
                        <button
                          className="hold-btn-confirm"
                          disabled={expired || !!actionLoading[hold.id]}
                          onClick={() => handleConfirm(hold.id)}
                        >
                          {actionLoading[hold.id] === 'confirm' ? 'Confirming...' : 'Confirm'}
                        </button>
                        <button
                          className="hold-btn-release"
                          disabled={!!actionLoading[hold.id]}
                          onClick={() => handleRelease(hold.id)}
                        >
                          {actionLoading[hold.id] === 'release' ? 'Releasing...' : 'Release'}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </section>
          )}

          {confirmedHolds.length > 0 && (
            <section>
              <h3 className="holds-section-title">Confirmed</h3>
              <div className="holds-grid">
                {confirmedHolds.map((hold) => {
                  const seat = seatMap[hold.seatId];
                  return (
                    <div key={hold.id} className="hold-card hold-card-confirmed">
                      <div className="hold-card-header">
                        <span className="hold-card-seat">
                          {seat
                            ? `${seat.section} — Row ${seat.rowLabel}, Seat ${seat.seatNumber}`
                            : `Seat ${hold.seatId.slice(0, 8)}...`}
                        </span>
                        <span className="hold-card-status confirmed">CONFIRMED</span>
                      </div>
                      <div className="hold-card-actions">
                        <button
                          className="hold-btn-release"
                          disabled={!!actionLoading[hold.id]}
                          onClick={() => handleRelease(hold.id)}
                        >
                          {actionLoading[hold.id] === 'release' ? 'Releasing...' : 'Release'}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
              <Link to="/checkout" state={{ holdIds: confirmedHolds.map((h) => h.id) }} className="checkout-link">
                Proceed to Checkout
              </Link>
            </section>
          )}
        </>
      )}
    </div>
  );
}

export default MyHolds;
