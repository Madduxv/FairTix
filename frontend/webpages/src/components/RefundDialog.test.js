import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import RefundDialog from './RefundDialog';
import api from '../api/client';

jest.mock('../api/client', () => ({ post: jest.fn() }));

const ticket = {
  orderId: 'order-1',
  eventTitle: 'Jazz Night',
  seatSection: 'A',
  seatRow: '1',
  seatNumber: '5',
  price: 50,
};

beforeEach(() => jest.clearAllMocks());

test('renders ticket details and policy text', () => {
  render(<RefundDialog ticket={ticket} onClose={jest.fn()} onSuccess={jest.fn()} />);
  expect(screen.getByText('Request a Refund')).toBeInTheDocument();
  expect(screen.getByText('Jazz Night')).toBeInTheDocument();
  expect(screen.getByText(/14 days of purchase/)).toBeInTheDocument();
});

test('blocks submit when reason is empty', () => {
  render(<RefundDialog ticket={ticket} onClose={jest.fn()} onSuccess={jest.fn()} />);
  const submit = screen.getByRole('button', { name: /submit request/i });
  expect(submit).toBeDisabled();
});

test('submits refund request and calls onSuccess', async () => {
  api.post.mockResolvedValue({});
  const onClose = jest.fn();
  const onSuccess = jest.fn();
  render(<RefundDialog ticket={ticket} onClose={onClose} onSuccess={onSuccess} />);
  fireEvent.change(screen.getByLabelText(/reason for refund/i), { target: { value: 'Changed plans' } });
  fireEvent.click(screen.getByRole('button', { name: /submit request/i }));
  await waitFor(() => expect(api.post).toHaveBeenCalledWith('/api/orders/order-1/refunds', { reason: 'Changed plans' }));
  await waitFor(() => expect(onSuccess).toHaveBeenCalled());
});

test('shows error when submit fails', async () => {
  api.post.mockRejectedValue(new Error('Server down'));
  render(<RefundDialog ticket={ticket} onClose={jest.fn()} onSuccess={jest.fn()} />);
  fireEvent.change(screen.getByLabelText(/reason for refund/i), { target: { value: 'Reason' } });
  fireEvent.click(screen.getByRole('button', { name: /submit request/i }));
  await waitFor(() => expect(screen.getByText('Server down')).toBeInTheDocument());
});
