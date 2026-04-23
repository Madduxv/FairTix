import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import '../styles/OrderHistory.css';

const STATUS_LABELS = {
  PENDING:   { label: 'Pending',   className: 'status-pending' },
  COMPLETED: { label: 'Completed', className: 'status-completed' },
  CANCELLED: { label: 'Cancelled', className: 'status-cancelled' },
  REFUNDED:  { label: 'Refunded',  className: 'status-refunded' },
};

function OrderHistoryPage() {
  useEffect(() => { document.title = 'Order History | FairTix'; }, []);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchOrders = useCallback(() => {
    setLoading(true);
    setError('');
    api.get('/api/orders')
      .then((data) => setOrders(data || []))
      .catch((err) => setError(err.message || 'Failed to load orders'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { fetchOrders(); }, [fetchOrders]);

  return (
    <div className="order-history">
      <h2>Order History</h2>

      {loading && (
        <div className="orders-list">
          {[1, 2, 3].map((i) => (
            <div key={i} className="order-card order-card-skeleton">
              <div className="skeleton-line" style={{ height: '1rem', width: '55%', marginBottom: '0.5rem' }} />
              <div className="skeleton-line" style={{ height: '0.8rem', width: '35%', marginBottom: '0.5rem' }} />
              <div className="skeleton-line" style={{ height: '0.8rem', width: '25%' }} />
            </div>
          ))}
        </div>
      )}

      {!loading && error && (
        <div className="orders-error">
          <p>{error}</p>
          <button onClick={fetchOrders}>Retry</button>
        </div>
      )}

      {!loading && !error && orders.length === 0 && (
        <div className="orders-empty">
          <p>You have no orders yet.</p>
          <p>Browse <Link to="/events">upcoming events</Link> to get started.</p>
        </div>
      )}

      {!loading && !error && orders.length > 0 && (
        <div className="orders-list">
          {orders.map((order) => {
            const s = STATUS_LABELS[order.status] || { label: order.status, className: '' };
            const eventDate = order.eventStartTime
              ? new Date(order.eventStartTime).toLocaleDateString(undefined, {
                  year: 'numeric', month: 'long', day: 'numeric',
                })
              : null;
            return (
              <div key={order.id} className="order-card">
                <div className="order-card-header">
                  <span className="order-event-title">
                    {order.eventTitle || 'Order'}
                  </span>
                  <span className={`order-status ${s.className}`}>{s.label}</span>
                </div>
                <div className="order-card-body">
                  {eventDate && (
                    <div>
                      <span className="label">Event Date</span>
                      <span>{eventDate}</span>
                    </div>
                  )}
                  <div>
                    <span className="label">Tickets</span>
                    <span>{order.ticketCount}</span>
                  </div>
                  {order.tickets && order.tickets.length > 0 && (
                    <div>
                      <span className="label">Seats</span>
                      <ul className="order-tickets-list">
                        {order.tickets.map((t) => (
                          <li key={t.id}>
                            {t.section} / Row {t.rowLabel} / Seat {t.seatNumber}
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                  <div>
                    <span className="label">Total</span>
                    <span>${Number(order.totalAmount).toFixed(2)} {order.currency}</span>
                  </div>
                  <div>
                    <span className="label">Ordered</span>
                    <span>{new Date(order.createdAt).toLocaleDateString()}</span>
                  </div>
                  <div>
                    <span className="label">Order ID</span>
                    <span style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>{order.id}</span>
                  </div>
                  {(order.status === 'COMPLETED' || order.status === 'REFUNDED') && (
                    <div>
                      <span className="label">Refund</span>
                      <Link to="/refunds">View Refund Requests</Link>
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

export default OrderHistoryPage;
