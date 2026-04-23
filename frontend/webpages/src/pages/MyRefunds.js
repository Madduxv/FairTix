import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import '../styles/MyRefunds.css';

const STATUS_LABELS = {
  REQUESTED: { label: 'Pending Review', className: 'status-requested' },
  APPROVED: { label: 'Approved', className: 'status-approved' },
  COMPLETED: { label: 'Completed', className: 'status-completed' },
  REJECTED: { label: 'Rejected', className: 'status-rejected' },
  CANCELLED: { label: 'Cancelled', className: 'status-cancelled' },
};

function MyRefunds() {
  useEffect(() => { document.title = 'My Refunds | FairTix'; }, []);
  const [refunds, setRefunds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchRefunds = useCallback(() => {
    setLoading(true);
    setError('');
    api.get('/api/refunds')
      .then((data) => setRefunds(data || []))
      .catch((err) => setError(err.message || 'Failed to load refunds'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { fetchRefunds(); }, [fetchRefunds]);

  return (
    <div className="my-refunds">
      <h2>My Refund Requests</h2>

      {loading && (
        <div className="refunds-list">
          {[1, 2, 3].map((i) => (
            <div key={i} className="refund-card refund-card-skeleton">
              <div className="skeleton-line" style={{ height: '1rem', width: '60%', marginBottom: '0.5rem' }} />
              <div className="skeleton-line" style={{ height: '0.8rem', width: '40%', marginBottom: '0.5rem' }} />
              <div className="skeleton-line" style={{ height: '0.8rem', width: '30%' }} />
            </div>
          ))}
        </div>
      )}

      {!loading && error && (
        <div className="refunds-error">
          <p>{error}</p>
          <button onClick={fetchRefunds}>Retry</button>
        </div>
      )}

      {!loading && !error && refunds.length === 0 && (
        <div className="refunds-empty">
          <p>You have no refund requests.</p>
          <p>To request a refund, go to <Link to="/my-tickets">My Tickets</Link> and click "Request Refund" on a valid ticket.</p>
        </div>
      )}

      {!loading && !error && refunds.length > 0 && (
        <div className="refunds-list">
          {refunds.map((refund) => {
            const s = STATUS_LABELS[refund.status] || { label: refund.status, className: '' };
            return (
              <div key={refund.id} className="refund-card">
                <div className="refund-card-header">
                  <span className="refund-order-id">Order: {refund.orderId}</span>
                  <span className={`refund-status ${s.className}`}>{s.label}</span>
                </div>
                <div className="refund-card-body">
                  <div>
                    <span className="label">Amount</span>
                    <span>${Number(refund.amount).toFixed(2)}</span>
                  </div>
                  <div>
                    <span className="label">Reason</span>
                    <span>{refund.reason}</span>
                  </div>
                  <div>
                    <span className="label">Submitted</span>
                    <span>{new Date(refund.createdAt).toLocaleDateString()}</span>
                  </div>
                  {refund.adminNotes && (
                    <div>
                      <span className="label">Notes</span>
                      <span>{refund.adminNotes}</span>
                    </div>
                  )}
                  {refund.completedAt && (
                    <div>
                      <span className="label">Completed</span>
                      <span>{new Date(refund.completedAt).toLocaleDateString()}</span>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default MyRefunds;
