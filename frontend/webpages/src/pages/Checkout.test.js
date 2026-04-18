import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Checkout from './Checkout';
import { AuthContext } from '../auth/AuthContext';
import api from '../api/client';

jest.mock('../components/Recaptcha', () => {
  const React = require('react');
  return React.forwardRef(function MockRecaptcha({ onChange }, ref) {
    React.useImperativeHandle(ref, () => ({ reset: jest.fn() }));
    return (
      <button data-testid="mock-captcha" type="button" onClick={() => onChange('test-captcha-token')}>
        Complete CAPTCHA
      </button>
    );
  });
});

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

test('auth:step-up-required event shows CAPTCHA verification modal', async () => {
  renderCheckout();
  await waitFor(() => expect(screen.getByLabelText('Card Number')).toBeInTheDocument());

  act(() => {
    window.dispatchEvent(
      new CustomEvent('auth:step-up-required', { detail: { action: 'CHECKOUT' } })
    );
  });

  await waitFor(() =>
    expect(screen.getByText(/additional verification required/i)).toBeInTheDocument()
  );
  expect(screen.getByTestId('mock-captcha')).toBeInTheDocument();
});

test('submitting CAPTCHA calls POST /auth/step-up/verify', async () => {
  api.post.mockResolvedValue({ orderId: 'order-1' });
  renderCheckout();
  await waitFor(() => expect(screen.getByLabelText('Card Number')).toBeInTheDocument());

  act(() => {
    window.dispatchEvent(
      new CustomEvent('auth:step-up-required', { detail: { action: 'CHECKOUT' } })
    );
  });
  await waitFor(() => expect(screen.getByTestId('mock-captcha')).toBeInTheDocument());

  fireEvent.click(screen.getByTestId('mock-captcha'));
  fireEvent.click(screen.getByRole('button', { name: /verify/i }));

  await waitFor(() =>
    expect(api.post).toHaveBeenCalledWith('/auth/step-up/verify', {
      captchaToken: 'test-captcha-token',
    })
  );
});

test('after successful verification original checkout is retried', async () => {
  api.post.mockResolvedValue({ orderId: 'order-1' });
  renderCheckout();
  await waitFor(() => expect(screen.getByLabelText('Card Number')).toBeInTheDocument());

  // Fill card and submit — simulate 428 by dispatching event and storing payload
  fireEvent.change(screen.getByLabelText('Card Number'), { target: { value: '4242424242424242' } });

  // Fake 428: reject checkout, dispatch step-up event
  api.post.mockRejectedValueOnce({ status: 428, code: 'STEP_UP_REQUIRED' });
  fireEvent.click(screen.getByRole('button', { name: /pay/i }));

  act(() => {
    window.dispatchEvent(
      new CustomEvent('auth:step-up-required', { detail: { action: 'CHECKOUT' } })
    );
  });
  await waitFor(() => expect(screen.getByTestId('mock-captcha')).toBeInTheDocument());

  api.post.mockResolvedValue({ orderId: 'order-retry' });
  fireEvent.click(screen.getByTestId('mock-captcha'));
  fireEvent.click(screen.getByRole('button', { name: /verify/i }));

  await waitFor(() => expect(screen.getByText('Order Confirmed!')).toBeInTheDocument());
});

test('failed CAPTCHA verification shows inline error', async () => {
  renderCheckout();
  await waitFor(() => expect(screen.getByLabelText('Card Number')).toBeInTheDocument());

  act(() => {
    window.dispatchEvent(
      new CustomEvent('auth:step-up-required', { detail: { action: 'CHECKOUT' } })
    );
  });
  await waitFor(() => expect(screen.getByTestId('mock-captcha')).toBeInTheDocument());

  api.post.mockRejectedValue({ message: 'CAPTCHA validation failed' });
  fireEvent.click(screen.getByTestId('mock-captcha'));
  fireEvent.click(screen.getByRole('button', { name: /verify/i }));

  await waitFor(() =>
    expect(screen.getByText('CAPTCHA validation failed')).toBeInTheDocument()
  );
});

test('submitting without completing CAPTCHA shows validation error', async () => {
  renderCheckout();
  await waitFor(() => expect(screen.getByLabelText('Card Number')).toBeInTheDocument());

  act(() => {
    window.dispatchEvent(
      new CustomEvent('auth:step-up-required', { detail: { action: 'CHECKOUT' } })
    );
  });
  await waitFor(() => expect(screen.getByRole('button', { name: /verify/i })).toBeInTheDocument());

  fireEvent.submit(screen.getByRole('button', { name: /verify/i }).closest('form'));

  await waitFor(() =>
    expect(screen.getByText('Please complete the CAPTCHA.')).toBeInTheDocument()
  );
});
