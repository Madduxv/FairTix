import { useState, useEffect, useCallback } from 'react';
import { useLocation, Link, useNavigate } from 'react-router-dom';
import api from '../api/client';
import '../styles/Checkout.css';

function Checkout() {
  const location = useLocation();
  const navigate = useNavigate();
  const [holds, setHolds] = useState([]);
  const [seatMap, setSeatMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [orderResult, setOrderResult] = useState(null);

  const fetchConfirmedHolds = useCallback(async () => {
    const passedHoldIds = location.state?.holdIds || [];
    setError('');
    try {
      let confirmedHolds;
      if (passedHoldIds.length > 0) {
        const fetched = await Promise.all(
          passedHoldIds.map(async (id) => {
            try {
              return await api.get(`/api/holds/${id}`);
            } catch {
              return null;
            }
          })
        );
        confirmedHolds = fetched.filter((h) => h && h.status === 'CONFIRMED');
      } else {
        const data = await api.get('/api/holds?status=CONFIRMED');
        confirmedHolds = data || [];
      }

      if (confirmedHolds.length === 0) {
        setError('No confirmed holds found. Please confirm your holds first.');
        setLoading(false);
        return;
      }

      setHolds(confirmedHolds);

      // Fetch seat details
      const eventIds = [...new Set(confirmedHolds.map((h) => h.eventId))];
      const entries = {};
      await Promise.all(
        eventIds.map(async (eid) => {
          const seats = await api.get(`/api/events/${eid}/seats`);
          for (const s of seats) {
            entries[s.id] = s;
          }
        })
      );
      setSeatMap(entries);
    } catch (err) {
      setError(err.message || 'Failed to load checkout details.');
    } finally {
      setLoading(false);
    }
  }, [location.state]);

  useEffect(() => {
    fetchConfirmedHolds();
  }, [fetchConfirmedHolds]);

  async function handlePlaceOrder() {
    if (submitting || holds.length === 0) return;
    setSubmitting(true);
    setError('');
    try {
      const order = await api.post('/api/orders', {
        holdIds: holds.map((h) => h.id),
      });
      setOrderResult(order);
    } catch (err) {
      setError(err.message || 'Failed to place order. Your holds may have expired.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <div className="loading">Loading checkout...</div>;

  if (orderResult) {
    return (
      <div className="checkout">
        <div className="checkout-success">
          <h2>Order Confirmed!</h2>
          <p>Your order has been placed and tickets have been issued.</p>
          <p className="checkout-order-id">Order ID: {orderResult.id}</p>
          <div className="checkout-success-actions">
            <Link to="/my-tickets" className="checkout-btn-primary">View My Tickets</Link>
            <Link to="/events" className="checkout-btn-secondary">Browse Events</Link>
          </div>
        </div>
      </div>
    );
  }

  if (error && holds.length === 0) {
    return (
      <div className="checkout">
        <div className="checkout-error-page">
          <p>{error}</p>
          <button onClick={() => navigate('/my-holds')} className="checkout-btn-secondary">
            Back to My Holds
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="checkout">
      <h2>Checkout</h2>

      {error && <div className="checkout-error">{error}</div>}

      <div className="checkout-summary">
        <h3>Order Summary</h3>
        <table className="checkout-table">
          <thead>
            <tr>
              <th>Section</th>
              <th>Row</th>
              <th>Seat</th>
            </tr>
          </thead>
          <tbody>
            {holds.map((hold) => {
              const seat = seatMap[hold.seatId];
              return (
                <tr key={hold.id}>
                  <td>{seat ? seat.section : '—'}</td>
                  <td>{seat ? seat.rowLabel : '—'}</td>
                  <td>{seat ? seat.seatNumber : hold.seatId.slice(0, 8) + '...'}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
        <div className="checkout-total">
          <span>{holds.length} seat{holds.length > 1 ? 's' : ''}</span>
          <span className="checkout-price">Free (MVP)</span>
        </div>
      </div>

      <div className="checkout-actions">
        <button className="checkout-btn-secondary" onClick={() => navigate('/my-holds')} disabled={submitting}>
          Back
        </button>
        <button className="checkout-btn-primary" onClick={handlePlaceOrder} disabled={submitting}>
          {submitting ? 'Placing Order...' : 'Place Order'}
        </button>
      </div>
    </div>
  );
}

export default Checkout;
