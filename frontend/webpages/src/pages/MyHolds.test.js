import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import MyHolds from './MyHolds';
import api from '../api/client';

jest.mock('../api/client', () => ({
  get: jest.fn(),
  post: jest.fn(),
}));

function renderMyHolds() {
  return render(
    <MemoryRouter>
      <MyHolds />
    </MemoryRouter>
  );
}

const activeSeat = { id: 'seat-1', section: 'A', rowLabel: '1', seatNumber: '5' };
const activeHold = {
  id: 'hold-1', seatId: 'seat-1', eventId: 'event-1', status: 'ACTIVE',
  expiresAt: new Date(Date.now() + 600000).toISOString(),
};
const confirmedHold = {
  id: 'hold-2', seatId: 'seat-1', eventId: 'event-1', status: 'CONFIRMED',
};

beforeEach(() => jest.clearAllMocks());

test('shows skeleton cards while loading', () => {
  api.get.mockReturnValue(new Promise(() => {}));
  renderMyHolds();
  expect(document.querySelector('.hold-card-skeleton')).toBeInTheDocument();
});

test('shows empty state when no holds exist', async () => {
  api.get.mockResolvedValue([]);
  renderMyHolds();
  await waitFor(() => expect(screen.getByText('No active holds.')).toBeInTheDocument());
});

test('renders active holds with countdown timer', async () => {
  api.get.mockImplementation((url) => {
    if (url.includes('status=ACTIVE')) return Promise.resolve([activeHold]);
    if (url.includes('status=CONFIRMED')) return Promise.resolve([]);
    if (url.includes('/seats')) return Promise.resolve([activeSeat]);
    return Promise.resolve([]);
  });
  renderMyHolds();
  await waitFor(() => expect(screen.getByText('Active')).toBeInTheDocument());
  expect(screen.getByText('Confirm')).toBeInTheDocument();
  expect(screen.getByText('Release')).toBeInTheDocument();
});

test('renders confirmed holds with checkout link', async () => {
  api.get.mockImplementation((url) => {
    if (url.includes('status=ACTIVE')) return Promise.resolve([]);
    if (url.includes('status=CONFIRMED')) return Promise.resolve([confirmedHold]);
    if (url.includes('/seats')) return Promise.resolve([activeSeat]);
    return Promise.resolve([]);
  });
  renderMyHolds();
  await waitFor(() => expect(screen.getByText('Confirmed')).toBeInTheDocument());
  expect(screen.getByText('CONFIRMED')).toBeInTheDocument();
  expect(screen.getByText('Proceed to Checkout')).toBeInTheDocument();
});

test('confirm action updates hold to confirmed', async () => {
  api.get.mockImplementation((url) => {
    if (url.includes('status=ACTIVE')) return Promise.resolve([activeHold]);
    if (url.includes('status=CONFIRMED')) return Promise.resolve([]);
    if (url.includes('/seats')) return Promise.resolve([activeSeat]);
    return Promise.resolve([]);
  });
  api.post.mockResolvedValue({ ...activeHold, status: 'CONFIRMED' });
  renderMyHolds();
  await waitFor(() => expect(screen.getByText('Confirm')).toBeInTheDocument());
  fireEvent.click(screen.getByText('Confirm'));
  await waitFor(() => expect(screen.getByText('Hold confirmed.')).toBeInTheDocument());
});

test('release action removes hold from list', async () => {
  api.get.mockImplementation((url) => {
    if (url.includes('status=ACTIVE')) return Promise.resolve([activeHold]);
    if (url.includes('status=CONFIRMED')) return Promise.resolve([]);
    if (url.includes('/seats')) return Promise.resolve([activeSeat]);
    return Promise.resolve([]);
  });
  api.post.mockResolvedValue({});
  renderMyHolds();
  await waitFor(() => expect(screen.getByText('Release')).toBeInTheDocument());
  fireEvent.click(screen.getByText('Release'));
  await waitFor(() => expect(screen.getByText('Hold released.')).toBeInTheDocument());
});

test('shows error when API fails', async () => {
  api.get.mockRejectedValue(new Error('Failed to load holds.'));
  renderMyHolds();
  await waitFor(() => expect(screen.getByText('Failed to load holds.')).toBeInTheDocument());
  expect(screen.getByText('Retry')).toBeInTheDocument();
});
