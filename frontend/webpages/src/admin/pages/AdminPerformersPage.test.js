import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import AdminPerformersPage from './AdminPerformersPage';
import api from '../../api/client';

jest.mock('../../api/client', () => ({
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
}));

jest.mock('../components/ConfirmDialog', () => {
  return function MockConfirmDialog({ open, onConfirm, onClose }) {
    if (!open) return null;
    return (
      <div data-testid="confirm-dialog">
        <button onClick={onConfirm}>Confirm</button>
        <button onClick={onClose}>Cancel</button>
      </div>
    );
  };
});

const performersResponse = {
  content: [
    { id: 'p1', name: 'The Beatles', genre: 'Rock', createdAt: '2026-01-01T00:00:00Z' },
    { id: 'p2', name: 'Mozart', genre: 'Classical', createdAt: '2026-01-02T00:00:00Z' },
  ],
  totalElements: 2,
};

beforeEach(() => jest.clearAllMocks());

test('performerTable_rendersFromApi renders table rows from mocked GET', async () => {
  api.get.mockResolvedValue(performersResponse);
  render(<AdminPerformersPage />);
  await waitFor(() => expect(screen.getByText('The Beatles')).toBeInTheDocument());
  expect(screen.getByText('Mozart')).toBeInTheDocument();
  expect(screen.getByText('Rock')).toBeInTheDocument();
  expect(screen.getByText('Classical')).toBeInTheDocument();
});

test('createDialog_submitsPost calls POST with form values', async () => {
  api.get.mockResolvedValue(performersResponse);
  api.post.mockResolvedValue({ id: 'p3', name: 'New Artist', genre: 'Jazz' });
  render(<AdminPerformersPage />);
  await waitFor(() => screen.getByText('The Beatles'));

  fireEvent.click(screen.getByRole('button', { name: /new performer/i }));
  fireEvent.change(screen.getByLabelText(/^name$/i), { target: { value: 'New Artist' } });
  fireEvent.change(screen.getByLabelText(/genre/i), { target: { value: 'Jazz' } });
  fireEvent.click(screen.getByRole('button', { name: /^create$/i }));

  await waitFor(() =>
    expect(api.post).toHaveBeenCalledWith('/api/performers', { name: 'New Artist', genre: 'Jazz' })
  );
});

test('editDialog_prefillsAndSubmitsPut pre-fills form and calls PUT', async () => {
  api.get.mockResolvedValue({
    content: [{ id: 'p1', name: 'The Beatles', genre: 'Rock', createdAt: '2026-01-01T00:00:00Z' }],
    totalElements: 1,
  });
  api.put.mockResolvedValue({});
  render(<AdminPerformersPage />);
  await waitFor(() => screen.getByText('The Beatles'));

  const row = screen.getByText('The Beatles').closest('tr');
  // First icon button in the row is Edit; second is Delete
  fireEvent.click(within(row).getAllByRole('button')[0]);

  await waitFor(() => expect(screen.getByDisplayValue('The Beatles')).toBeInTheDocument());
  expect(screen.getByDisplayValue('Rock')).toBeInTheDocument();

  fireEvent.change(screen.getByLabelText(/^name$/i), { target: { value: 'The Beatles (Remaster)' } });
  fireEvent.click(screen.getByRole('button', { name: /^update$/i }));

  await waitFor(() =>
    expect(api.put).toHaveBeenCalledWith('/api/performers/p1', {
      name: 'The Beatles (Remaster)',
      genre: 'Rock',
    })
  );
});

test('duplicateName_shows409Error surfaces conflict error in dialog', async () => {
  api.get.mockResolvedValue(performersResponse);
  const conflict = new Error('409 Conflict');
  conflict.status = 409;
  api.post.mockRejectedValue(conflict);
  render(<AdminPerformersPage />);
  await waitFor(() => screen.getByText('The Beatles'));

  fireEvent.click(screen.getByRole('button', { name: /new performer/i }));
  fireEvent.change(screen.getByLabelText(/^name$/i), { target: { value: 'The Beatles' } });
  fireEvent.click(screen.getByRole('button', { name: /^create$/i }));

  await waitFor(() =>
    expect(screen.getByText('A performer with this name already exists.')).toBeInTheDocument()
  );
});

test('deleteButton_callsDeleteEndpoint deletes performer after confirm', async () => {
  api.get.mockResolvedValue({
    content: [{ id: 'p1', name: 'The Beatles', genre: 'Rock', createdAt: '2026-01-01T00:00:00Z' }],
    totalElements: 1,
  });
  api.delete.mockResolvedValue(null);
  render(<AdminPerformersPage />);
  await waitFor(() => screen.getByText('The Beatles'));

  const row = screen.getByText('The Beatles').closest('tr');
  // Second icon button in the row is Delete
  fireEvent.click(within(row).getAllByRole('button')[1]);

  expect(screen.getByTestId('confirm-dialog')).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: /confirm/i }));

  await waitFor(() =>
    expect(api.delete).toHaveBeenCalledWith('/api/performers/p1')
  );
});
