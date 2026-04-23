import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import TransferDialog from './TransferDialog';
import RefundDialog from './RefundDialog';

function TicketCard({ ticket, onTransferred, onRefunded }) {
  const [showTransfer, setShowTransfer] = useState(false);
  const [showRefund, setShowRefund] = useState(false);
  const statusClass = ticket.status.toLowerCase();

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
      {ticket.status === 'VALID' && (
        <div className="ticket-card-actions">
          <button className="btn-transfer" onClick={() => setShowTransfer(true)}>
            Transfer
          </button>
          <button className="btn-refund" onClick={() => setShowRefund(true)}>
            Request Refund
          </button>
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
