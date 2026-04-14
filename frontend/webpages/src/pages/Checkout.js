import { useState, useEffect, useCallback, useRef } from 'react';
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

  const [cardNumber, setCardNumber] = useState('');
  const [paymentError, setPaymentError] = useState('');
  const [paymentState, setPaymentState] = useState('form'); // form | processing | success | failed
  const [tick, setTick] = useState(0);
  const tickRef = useRef(null);

  // Countdown timer for hold expiration
  useEffect(() => {
    if (holds.length > 0 && paymentState === 'form') {
      tickRef.current = setInterval(() => setTick((t) => t + 1), 1000);
      return () => clearInterval(tickRef.current);
    }
    return () => {};
  }, [holds.length, paymentState]);

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

  function getSeatPrice(hold) {
    const seat = seatMap[hold.seatId];
    return seat?.price ?? 0;
  }

  function calculateTotal() {
    return holds.reduce((sum, hold) => sum + getSeatPrice(hold), 0);
  }

  function formatCard(value) {
    const digits = value.replace(/\D/g, '').slice(0, 16);
    return digits.replace(/(\d{4})(?=\d)/g, '$1 ');
  }

  async function handlePayment(e) {
    e.preventDefault();
    const digits = cardNumber.replace(/\s/g, '');
    if (digits.length < 16) {
      setPaymentError('Please enter a valid 16-digit card number.');
      return;
    }

    setPaymentError('');
    setPaymentState('processing');
    setSubmitting(true);

    // Determine simulated outcome based on card number
    let simulatedOutcome = 'SUCCESS';
    if (digits.startsWith('4000')) simulatedOutcome = 'FAILURE';
    else if (digits.startsWith('4111')) simulatedOutcome = 'CANCELLED';

    try {
      const result = await api.post('/api/payments/checkout', {
        holdIds: holds.map((h) => h.id),
        simulatedOutcome,
      });
      setOrderResult(result);
      setPaymentState('success');
    } catch (err) {
      setPaymentState('failed');
      setPaymentError(
        err.body?.failureReason
          || err.message
          || 'Payment declined. Please check your card details or try a different card.'
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCancel() {
    // Record cancellation via the payment API before navigating away
    if (holds.length > 0 && paymentState !== 'failed') {
      try {
        setSubmitting(true);
        await api.post('/api/payments/checkout', {
          holdIds: holds.map((h) => h.id),
          simulatedOutcome: 'CANCELLED',
        });
      } catch {
        // Expected 402 — cancellation is recorded server-side regardless
      } finally {
        setSubmitting(false);
      }
    }
    navigate('/my-holds');
  }

  function handleRetry() {
    setPaymentState('form');
    setPaymentError('');
    setCardNumber('');
  }

  if (loading) return <div className="loading">Loading checkout...</div>;

  if (orderResult) {
    return (
      <div className="checkout">
        <div className="checkout-success">
          <h2>Order Confirmed!</h2>
          <p>Your payment was successful and tickets have been issued.</p>
          <p className="checkout-order-id">Order ID: {orderResult.orderId || orderResult.id}</p>
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

  const total = calculateTotal();

  // Check hold expiration
  const now = Date.now();
  const expiringHolds = holds.filter((h) => {
    if (!h.expiresAt) return false;
    const remaining = new Date(h.expiresAt).getTime() - now;
    return remaining > 0 && remaining < 120000; // < 2 minutes
  });
  const expiredHolds = holds.filter((h) => {
    if (!h.expiresAt) return false;
    return new Date(h.expiresAt).getTime() <= now;
  });

  return (
    <div className="checkout">
      <h2>Checkout</h2>

      {error && <div className="checkout-error">{error}</div>}

      {expiredHolds.length > 0 && (
        <div className="checkout-error">
          {expiredHolds.length} hold{expiredHolds.length > 1 ? 's have' : ' has'} expired.
          Please go back and create new holds.
          <button onClick={() => navigate('/my-holds')} className="checkout-btn-secondary" style={{ marginLeft: '0.5rem' }}>
            Back to My Holds
          </button>
        </div>
      )}

      {expiringHolds.length > 0 && expiredHolds.length === 0 && (
        <div className="checkout-warning">
          {expiringHolds.length} hold{expiringHolds.length > 1 ? 's are' : ' is'} expiring soon. Complete payment quickly.
        </div>
      )}

      <div className="checkout-summary">
        <h3>Order Summary</h3>
        <table className="checkout-table">
          <thead>
            <tr>
              <th>Section</th>
              <th>Row</th>
              <th>Seat</th>
              <th>Price</th>
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
                  <td>${getSeatPrice(hold).toFixed(2)}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
        <div className="checkout-total">
          <span>{holds.length} seat{holds.length > 1 ? 's' : ''}</span>
          <span className="checkout-price">${total.toFixed(2)}</span>
        </div>
      </div>

      {paymentState === 'processing' && (
        <div className="checkout-processing">
          <div className="checkout-spinner" />
          <p>Processing your payment...</p>
        </div>
      )}

      {paymentState === 'failed' && (
        <div className="checkout-payment-failed">
          <div className="checkout-error">{paymentError}</div>
          <div className="checkout-actions">
            <button className="checkout-btn-secondary" onClick={handleCancel}>
              Cancel
            </button>
            <button className="checkout-btn-primary" onClick={handleRetry}>
              Try Again
            </button>
          </div>
        </div>
      )}

      {paymentState === 'form' && (
        <form className="checkout-payment-form" onSubmit={handlePayment}>
          <h3>Payment Details</h3>
          {paymentError && <div className="checkout-error">{paymentError}</div>}
          <div className="checkout-field">
            <label htmlFor="cardNumber">Card Number</label>
            <input
              id="cardNumber"
              type="text"
              placeholder="4242 4242 4242 4242"
              value={cardNumber}
              onChange={(e) => setCardNumber(formatCard(e.target.value))}
              maxLength={19}
              required
              autoComplete="off"
            />
            <span className="checkout-field-hint">Use 4242... for success, 4000... for decline, 4111... for cancel</span>
          </div>
          <div className="checkout-actions">
            <button type="button" className="checkout-btn-secondary" onClick={handleCancel} disabled={submitting}>
              Cancel
            </button>
            <button type="submit" className="checkout-btn-primary" disabled={submitting}>
              Pay ${total.toFixed(2)}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}

export default Checkout;
