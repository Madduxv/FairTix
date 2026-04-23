import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import TransferDialog from './TransferDialog';
import RefundDialog from './RefundDialog';

function TicketCard({ ticket, onTransferred, onRefunded }) {
  const [showTransfer, setShowTransfer] = useState(false);
  const [showRefund, setShowRefund] = useState(false);
  const [showQr, setShowQr] = useState(false);
  const statusClass = ticket.status.toLowerCase();
  const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(ticket.id)}`;

  function downloadTicketArtifact() {
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

    const textX = PAD;
    const lineH = 22;
    let y = 105;
    ctx.font = '13px sans-serif';
    ctx.fillStyle = '#555';

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
      ctx.fillText(label + ':', textX, y);
      ctx.fillStyle = '#1a1a1a';
      ctx.fillText(value ?? '', textX + 90, y);
      y += lineH;
    }

    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      ctx.drawImage(img, W - QR - PAD, 56, QR, QR);
      trigger(canvas);
    };
    img.onerror = () => {
      ctx.strokeStyle = '#ccc';
      ctx.strokeRect(W - QR - PAD, 56, QR, QR);
      ctx.fillStyle = '#aaa';
      ctx.font = '11px sans-serif';
      ctx.fillText('QR unavailable', W - QR - PAD + 30, 56 + QR / 2);
      trigger(canvas);
    };
    img.src = qrUrl;
  }

  function trigger(canvas) {
    const a = document.createElement('a');
    a.href = canvas.toDataURL('image/png');
    a.download = `fairtix-ticket-${ticket.id}.png`;
    a.click();
  }

  return (
    <div className="ticket-card">
      <div className="ticket-card-header">
        <h3>
          <Link to={`/events/${ticket.eventId}`} className="ticket-event-link">
            {ticket.eventTitle}
          </Link>
        </h3>
        <span className={`ticket-status ${statusClass}`}>{ticket.status}</span>
      </div>
      <div className="ticket-card-details">
        <div>
          <span className="label">Venue</span>
          <span>{ticket.eventVenue}</span>
        </div>
        <div>
          <span className="label">Date</span>
          <span>{new Date(ticket.eventStartTime).toLocaleString()}</span>
        </div>
        <div className="ticket-card-seat">
          <span>
            <span className="label">Section</span>
            {ticket.seatSection}
          </span>
          <span>
            <span className="label">Row</span>
            {ticket.seatRow}
          </span>
          <span>
            <span className="label">Seat</span>
            {ticket.seatNumber}
          </span>
        </div>
        {ticket.price != null && (
          <div>
            <span className="label">Price</span>
            <span>${Number(ticket.price).toFixed(2)}</span>
          </div>
        )}
      </div>
      <div className="ticket-card-actions">
        <button className="btn-qr" onClick={() => setShowQr(v => !v)}>
          {showQr ? 'Hide QR' : 'Show QR'}
        </button>
        <a className="btn-calendar" href={`/api/tickets/${ticket.id}/calendar.ics`} download="fairtix-event.ics">
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
      {showQr && (
        <div className="ticket-qr-panel">
          <img
            src={qrUrl}
            alt="Ticket QR Code"
          />
          <a href={qrUrl} download="ticket-qr.png" className="btn-qr-download">
            Download
          </a>
        </div>
      )}
      {showTransfer && (
        <TransferDialog
          ticket={ticket}
          onClose={() => setShowTransfer(false)}
          onSuccess={() => { if (onTransferred) onTransferred(); }}
        />
      )}
      {showRefund && (
        <RefundDialog
          ticket={ticket}
          onClose={() => setShowRefund(false)}
          onSuccess={() => { if (onRefunded) onRefunded(); }}
        />
      )}
    </div>
  );
}

export default TicketCard;
