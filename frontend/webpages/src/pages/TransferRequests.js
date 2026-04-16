import React, { useState, useEffect, useCallback } from 'react';
import api from '../api/client';
import '../styles/MyTickets.css';

function TransferRequests() {
  const [tab, setTab] = useState('incoming');
  const [incoming, setIncoming] = useState([]);
  const [outgoing, setOutgoing] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');

  const fetchAll = useCallback(() => {
    setLoading(true);
    setError('');
    Promise.all([
      api.get('/api/transfers/incoming'),
      api.get('/api/transfers/outgoing'),
    ])
      .then(([inc, out]) => {
        setIncoming(inc || []);
        setOutgoing(out || []);
      })
      .catch((err) => setError(err.message || 'Failed to load transfers'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  function handleAction(requestId, action) {
    setActionError('');
    api.post(`/api/transfers/${requestId}/${action}`)
      .then(fetchAll)
      .catch((err) => setActionError(err.message || `Failed to ${action} transfer`));
  }

  function statusBadge(status) {
    return <span className={`ticket-status ${status.toLowerCase()}`}>{status}</span>;
  }

  return (
    <div className="transfer-requests">
      <h2>Transfer Requests</h2>
      <div className="transfer-tabs">
        <button
          className={`tab-btn${tab === 'incoming' ? ' active' : ''}`}
          onClick={() => setTab('incoming')}>
          Incoming {incoming.length > 0 && <span className="badge">{incoming.length}</span>}
        </button>
        <button
          className={`tab-btn${tab === 'outgoing' ? ' active' : ''}`}
          onClick={() => setTab('outgoing')}>
          Sent
        </button>
      </div>

      {loading && <p>Loading…</p>}
      {error && <p className="error-message">{error}</p>}
      {actionError && <p className="error-message">{actionError}</p>}

      {!loading && !error && tab === 'incoming' && (
        incoming.length === 0
          ? <p className="transfers-empty">No pending incoming transfer requests.</p>
          : <div className="transfers-list">
              {incoming.map((r) => (
                <div key={r.id} className="transfer-card">
                  <div className="transfer-card-info">
                    <strong>{r.eventTitle}</strong>
                    <span>Section {r.seatSection} · Row {r.seatRow} · Seat {r.seatNumber}</span>
                    <span>From: {r.fromEmail}</span>
                    <span>Expires: {new Date(r.expiresAt).toLocaleDateString()}</span>
                  </div>
                  <div className="transfer-card-actions">
                    <button className="btn-primary" onClick={() => handleAction(r.id, 'accept')}>Accept</button>
                    <button className="btn-secondary" onClick={() => handleAction(r.id, 'reject')}>Reject</button>
                  </div>
                </div>
              ))}
            </div>
      )}

      {!loading && !error && tab === 'outgoing' && (
        outgoing.length === 0
          ? <p className="transfers-empty">No outgoing transfer requests.</p>
          : <div className="transfers-list">
              {outgoing.map((r) => (
                <div key={r.id} className="transfer-card">
                  <div className="transfer-card-info">
                    <strong>{r.eventTitle}</strong>
                    <span>Section {r.seatSection} · Row {r.seatRow} · Seat {r.seatNumber}</span>
                    <span>To: {r.toEmail}</span>
                    {statusBadge(r.status)}
                  </div>
                  {r.status === 'PENDING' && (
                    <div className="transfer-card-actions">
                      <button className="btn-secondary" onClick={() => handleAction(r.id, 'cancel')}>Cancel</button>
                    </div>
                  )}
                </div>
              ))}
            </div>
      )}
    </div>
  );
}

export default TransferRequests;
