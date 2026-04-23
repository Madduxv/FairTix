import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import EventDetail from './EventDetail';
import { AuthContext } from '../auth/AuthContext';
import api from '../api/client';

jest.mock('../api/client', () => ({
  get: jest.fn(),
  post: jest.fn(),
}));

jest.mock('../components/WaitingRoom', () => {
  return function MockWaitingRoom() {
    return <div data-testid="waiting-room">Waiting Room</div>;
  };
});

jest.mock('../components/SeatMap', () => {
  return function MockSeatMap({ seats, onToggleSeat }) {
    return (
      <div data-testid="seat-map">
        {seats.map((s) => (
          <button key={s.id} onClick={() => onToggleSeat(s.id)}>
            {s.section}-{s.seatNumber}
          </button>
        ))}
      </div>
    );
  };
});

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useParams: () => ({ eventId: 'event-1' }),
}));

const activeEvent = {
  id: 'event-1', title: 'Jazz Night', venue: { name: 'Blue Note' },
  startTime: '2026-12-01T20:00:00Z', status: 'ACTIVE',
  queueRequired: false, maxTicketsPerUser: null,
};

const seat1 = { id: 'seat-1', section: 'A', rowLabel: '1', seatNumber: '1', price: 50, status: 'AVAILABLE' };
const seat2 = { id: 'seat-2', section: 'A', rowLabel: '1', seatNumber: '2', price: 50, status: 'SOLD' };

const authValue = {
  user: { userId: 'u1', email: 'test@example.com', role: 'USER', emailVerified: true },
  isLoading: false, login: jest.fn(), logout: jest.fn(),
  sessionExpired: false, clearSessionExpired: jest.fn(), refreshUser: jest.fn(),
};

function renderEventDetail(auth = authValue) {
  return render(
    <MemoryRouter initialEntries={['/events/event-1']}>
      <AuthContext.Provider value={auth}>
        <EventDetail />
      </AuthContext.Provider>
    </MemoryRouter>
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  api.get.mockImplementation((url) => {
    if (url === '/api/events/event-1') return Promise.resolve(activeEvent);
    if (url.includes('/seats')) return Promise.resolve([seat1, seat2]);
    if (url === '/api/tickets') return Promise.resolve([]);
    return Promise.reject(new Error('Unknown URL'));
  });
});

test('shows loading skeleton initially', () => {
  api.get.mockReturnValue(new Promise(() => {}));
  renderEventDetail();
  expect(document.querySelector('.event-detail-skeleton')).toBeInTheDocument();
});

test('renders event title and venue after loading', async () => {
  renderEventDetail();
  await waitFor(() => expect(screen.getByText('Jazz Night')).toBeInTheDocument());
  expect(screen.getByText('Blue Note')).toBeInTheDocument();
});

test('shows seat summary chips for ACTIVE events', async () => {
  renderEventDetail();
  await waitFor(() => expect(screen.getByText('2 total')).toBeInTheDocument());
  expect(screen.getByText('1 available')).toBeInTheDocument();
  expect(screen.getByText('1 sold')).toBeInTheDocument();
});

test('shows error when API fails', async () => {
  api.get.mockRejectedValue(new Error('Failed to load event details.'));
  renderEventDetail();
  await waitFor(() => expect(screen.getByText('Failed to load event details.')).toBeInTheDocument());
});

test('shows "Coming Soon" banner for PUBLISHED events', async () => {
  api.get.mockImplementation((url) => {
    if (url === '/api/events/event-1') return Promise.resolve({ ...activeEvent, status: 'PUBLISHED' });
    if (url.includes('/seats')) return Promise.resolve([]);
    return Promise.resolve([]);
  });
  renderEventDetail();
  await waitFor(() =>
    expect(screen.getByText(/tickets are not yet on sale/i)).toBeInTheDocument()
  );
});

test('shows cancelled banner with reason for CANCELLED events', async () => {
  api.get.mockImplementation((url) => {
    if (url === '/api/events/event-1') return Promise.resolve({ ...activeEvent, status: 'CANCELLED', cancellationReason: 'Weather' });
    if (url.includes('/seats')) return Promise.resolve([]);
    return Promise.resolve([]);
  });
  renderEventDetail();
  await waitFor(() => expect(screen.getByText(/this event has been cancelled/i)).toBeInTheDocument());
  expect(screen.getByText(/weather/i)).toBeInTheDocument();
});

test('shows completed banner for COMPLETED events', async () => {
  api.get.mockImplementation((url) => {
    if (url === '/api/events/event-1') return Promise.resolve({ ...activeEvent, status: 'COMPLETED' });
    if (url.includes('/seats')) return Promise.resolve([]);
    return Promise.resolve([]);
  });
  renderEventDetail();
  await waitFor(() =>
    expect(screen.getByText(/this event has already taken place/i)).toBeInTheDocument()
  );
});

test('seat row is clickable for AVAILABLE seats and shows selection bar', async () => {
  renderEventDetail();
  await waitFor(() => expect(screen.getByText('Jazz Night')).toBeInTheDocument());
  // Find a row with role="button" (available seats)
  const selectableRows = document.querySelectorAll('tr[role="button"]');
  expect(selectableRows.length).toBeGreaterThan(0);
  fireEvent.click(selectableRows[0]);
  await waitFor(() => expect(screen.getByText(/1 seat.* selected/i)).toBeInTheDocument());
});

test('map view toggle renders SeatMap component', async () => {
  renderEventDetail();
  await waitFor(() => expect(screen.getByText('Jazz Night')).toBeInTheDocument());
  fireEvent.click(screen.getByRole('button', { name: 'Map' }));
  expect(screen.getByTestId('seat-map')).toBeInTheDocument();
});

test('shows login prompt for unauthenticated users', async () => {
  const noAuth = { ...authValue, user: null };
  renderEventDetail(noAuth);
  await waitFor(() => expect(screen.getByText('Jazz Night')).toBeInTheDocument());
  expect(screen.getByText(/log in/i)).toBeInTheDocument();
});

test('displays Featuring section with performer name and genre', async () => {
  api.get.mockImplementation((url) => {
    if (url === '/api/events/event-1')
      return Promise.resolve({
        ...activeEvent,
        performers: [
          { id: 'p1', name: 'Famous Band', genre: 'Rock' },
          { id: 'p2', name: 'Solo Act', genre: null },
        ],
      });
    if (url.includes('/seats')) return Promise.resolve([seat1]);
    if (url === '/api/tickets') return Promise.resolve([]);
    return Promise.resolve([]);
  });
  renderEventDetail();
  await waitFor(() => expect(screen.getByText('Jazz Night')).toBeInTheDocument());
  expect(screen.getByText(/featuring/i)).toBeInTheDocument();
  expect(screen.getByText(/famous band \(rock\)/i)).toBeInTheDocument();
  expect(screen.getByText(/solo act/i)).toBeInTheDocument();
});

test('does not render Featuring section when event has no performers', async () => {
  api.get.mockImplementation((url) => {
    if (url === '/api/events/event-1')
      return Promise.resolve({ ...activeEvent, performers: [] });
    if (url.includes('/seats')) return Promise.resolve([seat1]);
    if (url === '/api/tickets') return Promise.resolve([]);
    return Promise.resolve([]);
  });
  renderEventDetail();
  await waitFor(() => expect(screen.getByText('Jazz Night')).toBeInTheDocument());
  expect(screen.queryByText(/featuring/i)).not.toBeInTheDocument();
});

test('does not render Featuring section when performers field is absent', async () => {
  api.get.mockImplementation((url) => {
    if (url === '/api/events/event-1')
      return Promise.resolve({ ...activeEvent }); // activeEvent has no performers field
    if (url.includes('/seats')) return Promise.resolve([seat1]);
    if (url === '/api/tickets') return Promise.resolve([]);
    return Promise.resolve([]);
  });
  renderEventDetail();
  await waitFor(() => expect(screen.getByText('Jazz Night')).toBeInTheDocument());
  expect(screen.queryByText(/featuring/i)).not.toBeInTheDocument();
});

test('shows purchase limit notice when maxTicketsPerUser is set', async () => {
  api.get.mockImplementation((url) => {
    if (url === '/api/events/event-1') return Promise.resolve({ ...activeEvent, maxTicketsPerUser: 2 });
    if (url.includes('/seats')) return Promise.resolve([seat1]);
    if (url === '/api/tickets') return Promise.resolve([]);
    return Promise.resolve([]);
  });
  renderEventDetail();
  await waitFor(() => expect(screen.getByText(/purchase limit/i)).toBeInTheDocument());
});

test('shows correct used/total count when user is at cap', async () => {
  const ownedTickets = [
    { eventId: 'event-1', status: 'ACTIVE' },
    { eventId: 'event-1', status: 'ACTIVE' },
  ];
  api.get.mockImplementation((url) => {
    if (url === '/api/events/event-1') return Promise.resolve({ ...activeEvent, maxTicketsPerUser: 2 });
    if (url.includes('/seats')) return Promise.resolve([seat1]);
    if (url === '/api/tickets') return Promise.resolve(ownedTickets);
    return Promise.resolve([]);
  });
  renderEventDetail();
  await waitFor(() => expect(screen.getByText(/2 \/ 2 ticket\(s\) used/i)).toBeInTheDocument());
});

test('seat rows are not selectable when user is at cap', async () => {
  const ownedTickets = [
    { eventId: 'event-1', status: 'ACTIVE' },
    { eventId: 'event-1', status: 'ACTIVE' },
  ];
  api.get.mockImplementation((url) => {
    if (url === '/api/events/event-1') return Promise.resolve({ ...activeEvent, maxTicketsPerUser: 2 });
    if (url.includes('/seats')) return Promise.resolve([seat1]);
    if (url === '/api/tickets') return Promise.resolve(ownedTickets);
    return Promise.resolve([]);
  });
  renderEventDetail();
  await waitFor(() => expect(screen.getByText('Jazz Night')).toBeInTheDocument());
  expect(document.querySelectorAll('tr[role="button"]').length).toBe(0);
});

test('seat rows are selectable when user is under cap', async () => {
  api.get.mockImplementation((url) => {
    if (url === '/api/events/event-1') return Promise.resolve({ ...activeEvent, maxTicketsPerUser: 2 });
    if (url.includes('/seats')) return Promise.resolve([seat1]);
    if (url === '/api/tickets') return Promise.resolve([{ eventId: 'event-1', status: 'ACTIVE' }]);
    return Promise.resolve([]);
  });
  renderEventDetail();
  await waitFor(() => expect(screen.getByText('Jazz Night')).toBeInTheDocument());
  expect(document.querySelectorAll('tr[role="button"]').length).toBeGreaterThan(0);
});

test('shows cap-reached error when selecting a seat would exceed the purchase cap', async () => {
  const seat3 = { id: 'seat-3', section: 'A', rowLabel: '1', seatNumber: '3', price: 50, status: 'AVAILABLE' };
  api.get.mockImplementation((url) => {
    if (url === '/api/events/event-1') return Promise.resolve({ ...activeEvent, maxTicketsPerUser: 1 });
    if (url.includes('/seats')) return Promise.resolve([seat1, seat3]);
    if (url === '/api/tickets') return Promise.resolve([]);
    return Promise.resolve([]);
  });
  renderEventDetail();
  await waitFor(() => expect(screen.getByText('Jazz Night')).toBeInTheDocument());
  const selectableRows = document.querySelectorAll('tr[role="button"]');
  fireEvent.click(selectableRows[0]);
  fireEvent.click(selectableRows[1]);
  await waitFor(() =>
    expect(screen.getByText(/purchase limit of 1 ticket/i)).toBeInTheDocument()
  );
});
