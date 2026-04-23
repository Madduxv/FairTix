import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import MyTickets from './MyTickets';
import api from '../api/client';

jest.mock('../api/client', () => ({
  get: jest.fn(),
  post: jest.fn(),
}));

jest.mock('../components/TicketCard', () => {
  return function MockTicketCard({ ticket }) {
    return <div data-testid="ticket-card">{ticket.eventTitle}</div>;
  };
});

function renderMyTickets() {
  return render(
    <MemoryRouter>
      <MyTickets />
    </MemoryRouter>
  );
}

beforeEach(() => jest.clearAllMocks());

test('renders page heading', async () => {
  api.get.mockResolvedValue([]);
  renderMyTickets();
  expect(screen.getByText('My Tickets')).toBeInTheDocument();
});

test('shows skeleton loading state while fetching', () => {
  api.get.mockReturnValue(new Promise(() => {}));
  renderMyTickets();
  expect(document.querySelector('.ticket-card-skeleton')).toBeInTheDocument();
});

test('renders a TicketCard for each ticket returned', async () => {
  api.get.mockResolvedValue([
    { id: 't1', eventTitle: 'Concert A', eventVenue: 'Arena', eventStartTime: '2026-12-01T18:00:00Z', status: 'VALID', seatSection: 'A', seatRow: '1', seatNumber: '5', price: 50 },
    { id: 't2', eventTitle: 'Concert B', eventVenue: 'Stadium', eventStartTime: '2026-12-02T18:00:00Z', status: 'VALID', seatSection: 'B', seatRow: '2', seatNumber: '3', price: 75 },
  ]);
  renderMyTickets();
  await waitFor(() => expect(screen.getAllByTestId('ticket-card')).toHaveLength(2));
  expect(screen.getByText('Concert A')).toBeInTheDocument();
  expect(screen.getByText('Concert B')).toBeInTheDocument();
});

test('shows empty state message with browse link when no tickets exist', async () => {
  api.get.mockResolvedValue([]);
  renderMyTickets();
  await waitFor(() =>
    expect(screen.getByText("You don't have any tickets yet.")).toBeInTheDocument()
  );
  expect(screen.getByRole('link', { name: /browse events/i })).toBeInTheDocument();
});

test('shows error message and retry button on API failure', async () => {
  api.get.mockRejectedValue(new Error('Failed to load tickets'));
  renderMyTickets();
  await waitFor(() =>
    expect(screen.getByText('Failed to load tickets')).toBeInTheDocument()
  );
  expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
});

test('retry button triggers a new API call', async () => {
  api.get
    .mockRejectedValueOnce(new Error('Network error'))
    .mockResolvedValueOnce([
      { id: 't1', eventTitle: 'Concert A', eventVenue: 'Arena', eventStartTime: '2026-12-01T18:00:00Z', status: 'VALID', seatSection: 'A', seatRow: '1', seatNumber: '5', price: 50 },
    ]);
  renderMyTickets();
  await waitFor(() => expect(screen.getByText('Network error')).toBeInTheDocument());
  fireEvent.click(screen.getByRole('button', { name: /retry/i }));
  await waitFor(() => expect(screen.getByTestId('ticket-card')).toBeInTheDocument());
});
