import React from 'react';
import { Link } from 'react-router-dom';

function TicketCard({ ticket }) {
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
    </div>
  );
}

export default TicketCard;
