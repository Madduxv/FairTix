import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import TicketCard from './TicketCard';

jest.mock('./TransferDialog', () => {
  return function MockTransferDialog({ onClose, onSuccess }) {
    return (
      <div data-testid="transfer-dialog">
        <button onClick={onClose}>Close Transfer</button>
        <button onClick={onSuccess}>Confirm Transfer</button>
      </div>
    );
  };
});

jest.mock('./RefundDialog', () => {
  return function MockRefundDialog({ onClose, onSuccess }) {
    return (
      <div data-testid="refund-dialog">
        <button onClick={onClose}>Close Refund</button>
        <button onClick={onSuccess}>Confirm Refund</button>
      </div>
    );
  };
});

const baseTicket = {
  id: 't1',
  eventId: 'e1',
  eventTitle: 'Rock Concert',
  eventVenue: 'City Arena',
  eventStartTime: '2026-12-01T18:00:00Z',
  status: 'VALID',
  seatSection: 'A',
  seatRow: '1',
  seatNumber: '5',
  price: 50.00,
};

function renderTicketCard(ticket = baseTicket, { onTransferred, onRefunded } = {}) {
  return render(
    <MemoryRouter>
      <TicketCard
        ticket={ticket}
        onTransferred={onTransferred || jest.fn()}
        onRefunded={onRefunded || jest.fn()}
      />
    </MemoryRouter>
  );
}

test('renders ticket event title, venue, and status', () => {
  renderTicketCard();
  expect(screen.getByText('Rock Concert')).toBeInTheDocument();
  expect(screen.getByText('City Arena')).toBeInTheDocument();
  expect(screen.getByText('VALID')).toBeInTheDocument();
});

test('renders seat details', () => {
  renderTicketCard();
  expect(screen.getByText('A')).toBeInTheDocument();   // section
  expect(screen.getByText('1')).toBeInTheDocument();   // row
  expect(screen.getByText('5')).toBeInTheDocument();   // seat number
});

test('renders formatted price', () => {
  renderTicketCard();
  expect(screen.getByText('$50.00')).toBeInTheDocument();
});

test('shows Transfer and Request Refund buttons for VALID ticket', () => {
  renderTicketCard();
  expect(screen.getByRole('button', { name: /transfer/i })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /request refund/i })).toBeInTheDocument();
});

test('does not show action buttons for non-VALID ticket status', () => {
  renderTicketCard({ ...baseTicket, status: 'USED' });
  expect(screen.queryByRole('button', { name: /transfer/i })).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: /request refund/i })).not.toBeInTheDocument();
});

test('does not show action buttons for CANCELLED ticket', () => {
  renderTicketCard({ ...baseTicket, status: 'CANCELLED' });
  expect(screen.queryByRole('button', { name: /transfer/i })).not.toBeInTheDocument();
});

test('TransferDialog is hidden by default and shown on Transfer click', () => {
  renderTicketCard();
  expect(screen.queryByTestId('transfer-dialog')).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: /transfer/i }));
  expect(screen.getByTestId('transfer-dialog')).toBeInTheDocument();
});

test('TransferDialog closes when onClose is called', () => {
  renderTicketCard();
  fireEvent.click(screen.getByRole('button', { name: /transfer/i }));
  fireEvent.click(screen.getByText('Close Transfer'));
  expect(screen.queryByTestId('transfer-dialog')).not.toBeInTheDocument();
});

test('calls onTransferred when transfer is confirmed', () => {
  const onTransferred = jest.fn();
  renderTicketCard(baseTicket, { onTransferred });
  fireEvent.click(screen.getByRole('button', { name: /transfer/i }));
  fireEvent.click(screen.getByText('Confirm Transfer'));
  expect(onTransferred).toHaveBeenCalled();
});

test('RefundDialog is shown on Request Refund click and closes on cancel', () => {
  renderTicketCard();
  expect(screen.queryByTestId('refund-dialog')).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: /request refund/i }));
  expect(screen.getByTestId('refund-dialog')).toBeInTheDocument();
  fireEvent.click(screen.getByText('Close Refund'));
  expect(screen.queryByTestId('refund-dialog')).not.toBeInTheDocument();
});

test('calls onRefunded when refund is confirmed', () => {
  const onRefunded = jest.fn();
  renderTicketCard(baseTicket, { onRefunded });
  fireEvent.click(screen.getByRole('button', { name: /request refund/i }));
  fireEvent.click(screen.getByText('Confirm Refund'));
  expect(onRefunded).toHaveBeenCalled();
});
