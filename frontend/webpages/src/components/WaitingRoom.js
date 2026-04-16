import { useState, useEffect, useCallback } from 'react';
import api from '../api/client';
import '../styles/WaitingRoom.css';

const POLL_INTERVAL_MS = 5000;

function WaitingRoom({ eventId, onAdmitted, onLeft }) {
  const [status, setStatus] = useState(null);
  const [error, setError] = useState('');
  const [leaving, setLeaving] = useState(false);

  const fetchStatus = useCallback(async () => {
    try {
      const data = await api.get(`/api/events/${eventId}/queue/status`);
      setStatus(data);
      if (data.status === 'ADMITTED') {
        onAdmitted(data.expiresAt);
      }
    } catch (err) {
      setError(err.message || 'Failed to get queue status.');
    }
  }, [eventId, onAdmitted]);

  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [fetchStatus]);

  async function handleLeave() {
    if (leaving) return;
    setLeaving(true);
    try {
      await api.delete(`/api/events/${eventId}/queue/leave`);
      onLeft();
    } catch (err) {
      setError(err.message || 'Failed to leave queue.');
      setLeaving(false);
    }
  }

  if (error) {
    return (
      <div className="waiting-room">
        <div className="waiting-room-error">{error}</div>
      </div>
    );
  }

  if (!status) {
    return (
      <div className="waiting-room">
        <div className="waiting-room-loading">Loading queue status...</div>
      </div>
    );
  }

  if (status.status === 'EXPIRED' || status.status === 'COMPLETED') {
    return (
      <div className="waiting-room">
        <div className="waiting-room-expired">
          <p>Your queue session has ended.</p>
          <button className="waiting-room-leave" onClick={onLeft}>Return to Event</button>
        </div>
      </div>
    );
  }

  const totalAhead = status.totalAhead ?? 0;

  return (
    <div className="waiting-room">
      <div className="waiting-room-card">
        <h3 className="waiting-room-title">You're in the queue</h3>
        <div className="waiting-room-position">
          <span className="waiting-room-number">#{status.position}</span>
          <span className="waiting-room-label">Your position</span>
        </div>
        {totalAhead > 0 && (
          <p className="waiting-room-ahead">{totalAhead} {totalAhead === 1 ? 'person' : 'people'} ahead of you</p>
        )}
        {totalAhead === 0 && status.status === 'WAITING' && (
          <p className="waiting-room-ahead">You're next!</p>
        )}
        <p className="waiting-room-hint">We'll automatically update your status. Please keep this page open.</p>
        <button
          className="waiting-room-leave"
          onClick={handleLeave}
          disabled={leaving}
        >
          {leaving ? 'Leaving...' : 'Leave Queue'}
        </button>
      </div>
    </div>
  );
}

export default WaitingRoom;
