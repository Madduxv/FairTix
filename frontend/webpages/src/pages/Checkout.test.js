import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Checkout from './Checkout';
import { AuthContext } from '../auth/AuthContext';
import api from '../api/client';

jest.mock('../api/client', () => ({
  get: jest.fn(),
  post: jest.fn(),
}));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useLocation: () => ({ state: { holdIds: ['hold-1'] } }),
}));

const verifiedUser = {
  userId: 'u1',
  email: 'test@example.com',
  role: 'USER',
  emailVerified: true,
};

const unverifiedUser = { ...verifiedUser, emailVerified: false };

function renderCheckout(user = verifiedUser) {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ user, login: jest.fn(), logout: jest.fn(), isLoading: false, sessionExpired: false, clearSessionExpired: jest.fn() }}>
        <Checkout />
      </AuthContext.Provider>
    </MemoryRouter>
  );
}

const holdData = { id: 'hold-1', seatId: 'seat-1', eventId: 'event-1', status: 'CONFIRMED', expiresAt: '2099-12-31T23:59:59Z' };
const seatData = { id: 'seat-1', section: 'A', rowLabel: '1', seatNumber: '5', price: 50 };

beforeEach(() => {
  jest.clearAllMocks();
  api.get.mockImplementation((url) => {
    if (url.includes('/api/holds/')) return Promise.resolve(holdData);
    if (url.includes('/seats')) return Promise.resolve([seatData]);
    return Promise.resolve([]);
  });
});

test('shows loading state initially', () => {
  api.get.mockReturnValue(new Promise(() => {}));
  renderCheckout();
  expect(screen.getByText('Loading checkout...')).toBeInTheDocument();
});

test('shows email verification required for unverified users', () => {
  renderCheckout(unverifiedUser);
  expect(screen.getByText('Email verification required')).toBeInTheDocument();
});

test('renders order summary with seat details after loading', async () => {
  renderCheckout();
  await waitFor(() => expect(screen.getByText('Checkout')).toBeInTheDocument());
  expect(screen.getByText('A')).toBeInTheDocument();
  expect(screen.getAllByText('$50.00').length).toBeGreaterThanOrEqual(1);
});

test('shows error when no confirmed holds found', async () => {
  api.get.mockImplementation((url) => {
    if (url.includes('/api/holds/')) return Promise.resolve({ ...holdData, status: 'EXPIRED' });
    if (url.includes('/seats')) return Promise.resolve([]);
    return Promise.resolve([]);
  });
  renderCheckout();
  await waitFor(() =>
    expect(screen.getByText(/no confirmed holds found/i)).toBeInTheDocument(),
    { timeout: 3000 }
  );
});

test('validates card number length before payment', async () => {
  renderCheckout();
  await waitFor(() => expect(screen.getByText('Checkout')).toBeInTheDocument());
  const input = screen.getByLabelText('Card Number');
  fireEvent.change(input, { target: { value: '4242 4242' } });
  fireEvent.click(screen.getByRole('button', { name: /pay/i }));
  await waitFor(() =>
    expect(screen.getByText('Please enter a valid 16-digit card number.')).toBeInTheDocument()
  );
});

test('shows success screen after successful payment', async () => {
  api.post.mockResolvedValue({ orderId: 'order-123' });
  renderCheckout();
  await waitFor(() => expect(screen.getByLabelText('Card Number')).toBeInTheDocument());
  fireEvent.change(screen.getByLabelText('Card Number'), { target: { value: '4242424242424242' } });
  fireEvent.click(screen.getByRole('button', { name: /pay/i }));
  await waitFor(() => expect(screen.getByText('Order Confirmed!')).toBeInTheDocument());
  expect(screen.getByText(/order-123/)).toBeInTheDocument();
  expect(screen.getByText(/confirmation email/i)).toBeInTheDocument();
});

test('shows failure screen with retry option on payment decline', async () => {
  api.post.mockRejectedValue({ message: 'Card declined', body: { failureReason: 'Insufficient funds' } });
  renderCheckout();
  await waitFor(() => expect(screen.getByLabelText('Card Number')).toBeInTheDocument());
  fireEvent.change(screen.getByLabelText('Card Number'), { target: { value: '4000000000000000' } });
  fireEvent.click(screen.getByRole('button', { name: /pay/i }));
  await waitFor(() => expect(screen.getByText('Insufficient funds')).toBeInTheDocument());
  expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
});

test('retry resets to payment form', async () => {
  api.post.mockRejectedValueOnce({ message: 'declined', body: { failureReason: 'Declined' } });
  renderCheckout();
  await waitFor(() => expect(screen.getByLabelText('Card Number')).toBeInTheDocument());
  fireEvent.change(screen.getByLabelText('Card Number'), { target: { value: '4000000000000000' } });
  fireEvent.click(screen.getByRole('button', { name: /pay/i }));
  await waitFor(() => expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument());
  fireEvent.click(screen.getByRole('button', { name: /try again/i }));
  expect(screen.getByLabelText('Card Number')).toBeInTheDocument();
});

test('cancel navigates to my-holds', async () => {
  api.post.mockRejectedValue({ status: 402 });
  renderCheckout();
  await waitFor(() => expect(screen.getByLabelText('Card Number')).toBeInTheDocument());
  fireEvent.click(screen.getByRole('button', { name: /cancel/i }));
  await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/my-holds'));
});
