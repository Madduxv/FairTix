import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import TransferDialog from './TransferDialog';
import api from '../api/client';

jest.mock('../api/client', () => ({
  post: jest.fn(),
}));

const baseTicket = {
  id: 't1',
  eventTitle: 'Rock Concert',
  seatSection: 'A',
  seatRow: '1',
  seatNumber: '5',
};

function renderDialog({ onClose, onSuccess } = {}) {
  return render(
    <TransferDialog
      ticket={baseTicket}
      onClose={onClose || jest.fn()}
      onSuccess={onSuccess || jest.fn()}
    />
  );
}

beforeEach(() => jest.clearAllMocks());

test('renders ticket summary and email input', () => {
  renderDialog();
  expect(screen.getByText('Rock Concert')).toBeInTheDocument();
  expect(screen.getByLabelText('Recipient email')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /send transfer request/i })).toBeInTheDocument();
});

test('calls onSuccess and onClose on successful transfer', async () => {
  api.post.mockResolvedValue({});
  const onClose = jest.fn();
  const onSuccess = jest.fn();
  renderDialog({ onClose, onSuccess });
  fireEvent.change(screen.getByLabelText('Recipient email'), { target: { value: 'friend@example.com' } });
  fireEvent.click(screen.getByRole('button', { name: /send transfer request/i }));
  await waitFor(() => expect(onSuccess).toHaveBeenCalled());
  expect(onClose).toHaveBeenCalled();
});

test('shows error on failed transfer', async () => {
  api.post.mockRejectedValue(new Error('User not found'));
  renderDialog();
  fireEvent.change(screen.getByLabelText('Recipient email'), { target: { value: 'nobody@example.com' } });
  fireEvent.click(screen.getByRole('button', { name: /send transfer request/i }));
  await waitFor(() => expect(screen.getByText('User not found')).toBeInTheDocument());
});

test('cancel button calls onClose', () => {
  const onClose = jest.fn();
  renderDialog({ onClose });
  fireEvent.click(screen.getByRole('button', { name: /cancel/i }));
  expect(onClose).toHaveBeenCalled();
});

test('disables submit button while submitting', async () => {
  api.post.mockReturnValue(new Promise(() => {}));
  renderDialog();
  fireEvent.change(screen.getByLabelText('Recipient email'), { target: { value: 'friend@example.com' } });
  fireEvent.click(screen.getByRole('button', { name: /send transfer request/i }));
  await waitFor(() => expect(screen.getByText('Sending…')).toBeInTheDocument());
});
