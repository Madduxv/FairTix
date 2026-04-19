import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import AdminFraudPage from './AdminFraudPage';
import api from '../../api/client';

jest.mock('../../api/client', () => ({
  get: jest.fn(),
  patch: jest.fn(),
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

const flagsResponse = {
  content: [
    {
      id: 'flag1',
      userId: 'user-uuid-1',
      flagType: 'RAPID_PURCHASE',
      severity: 'HIGH',
      details: 'Bought 10 tickets in 2 minutes',
      createdAt: '2026-04-01T10:00:00Z',
      status: 'OPEN',
    },
  ],
  totalElements: 1,
};

beforeEach(() => jest.clearAllMocks());

test('flagsTable_rendersFromApi renders table rows from mocked GET', async () => {
  api.get.mockResolvedValue(flagsResponse);
  render(<AdminFraudPage />);
  await waitFor(() => expect(screen.getByText('RAPID_PURCHASE')).toBeInTheDocument());
  expect(screen.getByText('user-uuid-1')).toBeInTheDocument();
});

test('resolveButton_callsPatchEndpoint patches correct flag after confirm', async () => {
  api.get.mockResolvedValue(flagsResponse);
  api.patch.mockResolvedValue({});
  render(<AdminFraudPage />);
  await waitFor(() => screen.getByRole('button', { name: /resolve/i }));

  fireEvent.click(screen.getByRole('button', { name: /resolve/i }));
  fireEvent.click(screen.getByRole('button', { name: /confirm/i }));

  await waitFor(() =>
    expect(api.patch).toHaveBeenCalledWith('/api/admin/fraud/flags/flag1/resolve')
  );
});

test('riskLookup_rendersScoreAndTier shows score and tier on valid user', async () => {
  api.get
    .mockResolvedValueOnce(flagsResponse)
    .mockResolvedValueOnce({
      score: 72,
      tier: 'HIGH',
      flagCount: 3,
      lastCalculated: '2026-04-10T08:00:00Z',
    });
  render(<AdminFraudPage />);
  await waitFor(() => screen.getByText('RAPID_PURCHASE'));

  const [, riskInput] = screen.getAllByPlaceholderText('Enter exact user UUID');
  fireEvent.change(riskInput, { target: { value: 'user-uuid-1' } });
  fireEvent.click(screen.getByRole('button', { name: /lookup/i }));

  await waitFor(() => expect(screen.getByText('72')).toBeInTheDocument());
  expect(screen.getByText('3')).toBeInTheDocument();
});

test('riskLookup_shows404Error shows inline error when no record exists', async () => {
  const notFound = new Error('Not found');
  notFound.status = 404;
  api.get
    .mockResolvedValueOnce(flagsResponse)
    .mockRejectedValueOnce(notFound);
  render(<AdminFraudPage />);
  await waitFor(() => screen.getByText('RAPID_PURCHASE'));

  const [, riskInput] = screen.getAllByPlaceholderText('Enter exact user UUID');
  fireEvent.change(riskInput, { target: { value: 'unknown-user' } });
  fireEvent.click(screen.getByRole('button', { name: /lookup/i }));

  await waitFor(() =>
    expect(screen.getByText('No risk score record found for this user.')).toBeInTheDocument()
  );
});

test('severityFilter_sendsCorrectParam includes severity in API request', async () => {
  api.get.mockResolvedValue({ content: [], totalElements: 0 });
  render(<AdminFraudPage />);
  await waitFor(() => screen.getByText('No flags found.'));

  fireEvent.mouseDown(screen.getByRole('combobox'));
  const highOption = await screen.findByRole('option', { name: 'HIGH' });
  fireEvent.click(highOption);

  api.get.mockClear();
  api.get.mockResolvedValue({ content: [], totalElements: 0 });
  fireEvent.click(screen.getByRole('button', { name: /search/i }));

  await waitFor(() =>
    expect(api.get).toHaveBeenCalledWith(expect.stringContaining('severity=HIGH'))
  );
});
