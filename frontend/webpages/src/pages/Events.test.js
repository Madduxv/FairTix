import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Events from './Events';
import api from '../api/client';

jest.mock('../api/client', () => ({
  get: jest.fn(),
}));

function eventsPage(events = [], totalPages = 1, totalElements = events.length) {
  return { content: events, page: { totalPages, totalElements } };
}

function renderEvents() {
  return render(
    <MemoryRouter>
      <Events />
    </MemoryRouter>
  );
}

beforeEach(() => jest.clearAllMocks());

test('shows skeleton cards while loading', () => {
  api.get.mockReturnValue(new Promise(() => {}));
  renderEvents();
  expect(document.querySelector('.event-card-skeleton')).toBeInTheDocument();
});

test('renders event titles after fetch resolves', async () => {
  api.get.mockResolvedValue(eventsPage([
    { id: 'e1', title: 'Jazz Night', venue: { name: 'Blue Note' }, startTime: '2026-12-01T20:00:00Z', status: 'ACTIVE' },
    { id: 'e2', title: 'Rock Concert', venue: { name: 'Madison SQ' }, startTime: '2026-12-15T19:00:00Z', status: 'PUBLISHED' },
  ], 1, 2));
  renderEvents();
  await waitFor(() => expect(screen.getByText('Jazz Night')).toBeInTheDocument(), { timeout: 2000 });
  expect(screen.getByText('Rock Concert')).toBeInTheDocument();
});

test('shows "On Sale" badge for ACTIVE events', async () => {
  api.get.mockResolvedValue(eventsPage([
    { id: 'e1', title: 'Jazz Night', venue: { name: 'Blue Note' }, startTime: '2026-12-01T20:00:00Z', status: 'ACTIVE' },
  ]));
  renderEvents();
  await waitFor(() => expect(screen.getByText('On Sale')).toBeInTheDocument(), { timeout: 2000 });
});

test('shows "Coming Soon" badge for PUBLISHED events', async () => {
  api.get.mockResolvedValue(eventsPage([
    { id: 'e2', title: 'Rock Concert', venue: { name: 'Madison SQ' }, startTime: '2026-12-15T19:00:00Z', status: 'PUBLISHED' },
  ]));
  renderEvents();
  await waitFor(() => expect(screen.getByText('Coming Soon')).toBeInTheDocument(), { timeout: 2000 });
});

test('shows empty state when no events match', async () => {
  api.get.mockResolvedValue(eventsPage([]));
  renderEvents();
  await waitFor(() =>
    expect(screen.getByText('No events match your search.')).toBeInTheDocument(),
    { timeout: 2000 }
  );
});

test('shows error message when API call fails', async () => {
  api.get.mockRejectedValue(new Error('Failed to load events'));
  renderEvents();
  await waitFor(() =>
    expect(screen.getByText('Failed to load events')).toBeInTheDocument(),
    { timeout: 2000 }
  );
});

test('search inputs are rendered', () => {
  api.get.mockReturnValue(new Promise(() => {}));
  renderEvents();
  expect(screen.getByPlaceholderText('Search by title...')).toBeInTheDocument();
  expect(screen.getByPlaceholderText('Filter by venue...')).toBeInTheDocument();
});

test('"Show past events" checkbox toggles filter', async () => {
  api.get.mockResolvedValue(eventsPage([]));
  renderEvents();
  const checkbox = screen.getByRole('checkbox', { name: /show past events/i });
  expect(checkbox).not.toBeChecked();
  fireEvent.click(checkbox);
  expect(checkbox).toBeChecked();
});
