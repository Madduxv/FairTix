import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api from '../api/client';
import TransferDialog from '../components/TransferDialog';
import RefundDialog from '../components/RefundDialog';
import '../styles/MyTickets.css';
import '../styles/TicketDetailPage.css';

function TicketDetailPage() {
  useEffect(() => { document.title = 'Ticket Details | FairTix'; }, []);
  const { ticketId } = useParams();
  const navigate = useNavigate();
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showTransfer, setShowTransfer] = useState(false);
  const [showRefund, setShowRefund] = useState(false);

  const fetchTicket = useCallback(() => {
    setLoading(true);
    setError('');
    api.get(`/api/tickets/${ticketId}`)
      .then(setTicket)
      .catch((err) => setError(err.message || 'Failed to load ticket'))
      .finally(() => setLoading(false));
  }, [ticketId]);

  useEffect(() => { fetchTicket(); }, [fetchTicket]);

  const qrUrl = ticket
    ? `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(ticket.id)}`
    : '';

  function downloadTicketArtifact() {
    if (!ticket) return;
    const W = 600, H = 320, PAD = 24, QR = 180;
    const canvas = document.createElement('canvas');
    canvas.width = W;
    canvas.height = H;
    const ctx = canvas.getContext('2d');

    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, W, H);
    ctx.strokeStyle = '#1a73e8';
    ctx.lineWidth = 3;
    ctx.strokeRect(2, 2, W - 4, H - 4);

    ctx.fillStyle = '#1a73e8';
    ctx.fillRect(0, 0, W, 48);
    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 18px sans-serif';
    ctx.fillText('FairTix', PAD, 30);

    ctx.fillStyle = '#1a1a1a';
    ctx.font = 'bold 16px sans-serif';
    const title = ticket.eventTitle || '';
    ctx.fillText(title.length > 45 ? title.slice(0, 42) + '…' : title, PAD, 76);

    const lineH = 22;
    let y = 105;
    ctx.font = '13px sans-serif';

    const lines = [
      ['Venue', ticket.eventVenue],
      ['Date', ticket.eventStartTime ? new Date(ticket.eventStartTime).toLocaleString() : ''],
      ['Section', ticket.seatSection],
      ['Row', ticket.seatRow],
      ['Seat', ticket.seatNumber],
      ['Ticket ID', ticket.id],
      ['Order Ref', ticket.orderId],
    ];
    for (const [label, value] of lines) {
      ctx.fillStyle = '#888';
      ctx.fillText(label + ':', PAD, y);
      ctx.fillStyle = '#1a1a1a';
      ctx.fillText(value ?? '', PAD + 90, y);
      y += lineH;
    }

    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => { ctx.drawImage(img, W - QR - PAD, 56, QR, QR); triggerDownload(canvas); };
    img.onerror = () => { triggerDownload(canvas); };
    img.src = qrUrl;
  }

  function triggerDownload(canvas) {
    const a = document.createElement('a');
    a.href = canvas.toDataURL('image/png');
    a.download = `fairtix-ticket-${ticket.id}.png`;
    a.click();
  }

  if (loading) {
    return (
      <div className="ticket-detail-page">
        <div className="ticket-detail-skeleton">
          <div className="skeleton-line skeleton-detail-title" />
          <div className="skeleton-line skeleton-detail-meta" />
          <div className="skeleton-line skeleton-detail-meta" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="ticket-detail-page">
        <div className="ticket-detail-error">
          <p>{error}</p>
          <button onClick={fetchTicket} className="tickets-retry">Retry</button>
        </div>
      </div>
    );
  }

  if (!ticket) return null;

  const statusClass = ticket.status.toLowerCase();

  return (
    <div className="ticket-detail-page">
      <div className="ticket-detail-back">
        <Link to="/my-tickets" className="ticket-detail-back-link">← My Tickets</Link>
      </div>

      <div className="ticket-detail-card">
        <div className="ticket-detail-header">
          <div>
            <h2>
              <Link to={`/events/${ticket.eventId}`} className="ticket-event-link">
                {ticket.eventTitle}
              </Link>
            </h2>
            <p className="ticket-detail-venue">{ticket.eventVenue}</p>
          </div>
          <span className={`ticket-status ${statusClass}`}>{ticket.status}</span>
        </div>

        <div className="ticket-detail-body">
          <div className="ticket-detail-sections">
            <section className="ticket-detail-section">
              <h4>Event</h4>
              <div className="ticket-detail-row">
                <span className="label">Date</span>
                <span>{new Date(ticket.eventStartTime).toLocaleString()}</span>
              </div>
              <div className="ticket-detail-row">
                <span className="label">Venue</span>
                <span>{ticket.eventVenue}</span>
              </div>
            </section>

            <section className="ticket-detail-section">
              <h4>Seat</h4>
              <div className="ticket-detail-seat-row">
                <div className="ticket-detail-row">
                  <span className="label">Section</span>
                  <span>{ticket.seatSection}</span>
                </div>
                <div className="ticket-detail-row">
                  <span className="label">Row</span>
                  <span>{ticket.seatRow}</span>
                </div>
                <div className="ticket-detail-row">
                  <span className="label">Seat</span>
                  <span>{ticket.seatNumber}</span>
                </div>
              </div>
              {ticket.price != null && (
                <div className="ticket-detail-row">
                  <span className="label">Price</span>
                  <span>${Number(ticket.price).toFixed(2)}</span>
                </div>
              )}
            </section>

            <section className="ticket-detail-section">
              <h4>Ticket Info</h4>
              <div className="ticket-detail-row">
                <span className="label">Holder</span>
                <span>{ticket.holderEmail}</span>
              </div>
              <div className="ticket-detail-row">
                <span className="label">Issued</span>
                <span>{new Date(ticket.issuedAt).toLocaleString()}</span>
              </div>
              <div className="ticket-detail-row">
                <span className="label">Ticket ID</span>
                <span className="ticket-detail-id">{ticket.id}</span>
              </div>
            </section>

            <section className="ticket-detail-section">
              <h4>Order</h4>
              <div className="ticket-detail-row">
                <span className="label">Order Ref</span>
                <span className="ticket-detail-id">{ticket.orderId}</span>
              </div>
            </section>
          </div>

          <div className="ticket-detail-qr">
            <img src={qrUrl} alt="Ticket QR Code" className="ticket-detail-qr-img" />
            <a href={qrUrl} download="ticket-qr.png" className="btn-qr">Download QR</a>
          </div>
        </div>

        <div className="ticket-card-actions ticket-detail-actions">
          <a
            className="btn-calendar"
            href={`/api/tickets/${ticket.id}/calendar.ics`}
            download="fairtix-event.ics"
          >
            Add to Calendar
          </a>
          <button className="download-ticket-btn" onClick={downloadTicketArtifact}>
            Download Ticket
          </button>
          {ticket.status === 'VALID' && (
            <>
              <button className="btn-transfer" onClick={() => setShowTransfer(true)}>
                Transfer
              </button>
              <button className="btn-refund" onClick={() => setShowRefund(true)}>
                Request Refund
              </button>
            </>
          )}
        </div>
      </div>

      {showTransfer && (
        <TransferDialog
          ticket={ticket}
          onClose={() => setShowTransfer(false)}
          onSuccess={() => { setShowTransfer(false); fetchTicket(); }}
        />
      )}
      {showRefund && (
        <RefundDialog
          ticket={ticket}
          onClose={() => setShowRefund(false)}
          onSuccess={() => { navigate('/my-tickets'); }}
        />
      )}
    </div>
  );
}

export default TicketDetailPage;
