import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Login from './Login';
import { AuthContext } from '../auth/AuthContext';

// Mock useNavigate so we don't need a full router history
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => jest.fn(),
  useLocation: () => ({ state: null }),
}));

function makeError(status, message = 'error', body = {}) {
  const err = new Error(message);
  err.status = status;
  err.body = body;
  return err;
}

function renderLogin(loginFn) {
  const contextValue = {
    user: null,
    login: loginFn,
    logout: jest.fn(),
    isLoading: false,
    sessionExpired: false,
    clearSessionExpired: jest.fn(),
  };
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={contextValue}>
        <Login />
      </AuthContext.Provider>
    </MemoryRouter>
  );
}

async function submitForm(email = 'test@example.com', password = 'password') {
  const emailInput = screen.getByLabelText(/email/i);
  fireEvent.change(emailInput, { target: { value: email } });
  fireEvent.change(screen.getByLabelText(/password/i), { target: { value: password } });
  await act(async () => {
    fireEvent.submit(emailInput.closest('form'));
  });
}

test('shows invalid credentials message on 401', async () => {
  renderLogin(jest.fn().mockRejectedValue(makeError(401)));
  await submitForm();
  await waitFor(() =>
    expect(screen.getByText('Invalid email or password.')).toBeInTheDocument()
  );
});

test('shows invalid credentials message on 400', async () => {
  renderLogin(jest.fn().mockRejectedValue(makeError(400)));
  await submitForm();
  await waitFor(() =>
    expect(screen.getByText('Invalid email or password.')).toBeInTheDocument()
  );
});

test('shows server error message on 500', async () => {
  renderLogin(jest.fn().mockRejectedValue(makeError(500)));
  await submitForm();
  await waitFor(() =>
    expect(screen.getByText('Server error. Please try again later.')).toBeInTheDocument()
  );
});

test('shows network error message when no status', async () => {
  renderLogin(jest.fn().mockRejectedValue(new Error('fetch failed')));
  await submitForm();
  await waitFor(() =>
    expect(screen.getByText('Network error. Please check your connection.')).toBeInTheDocument()
  );
});

test('shows lockout message and timer on 429', async () => {
  renderLogin(
    jest.fn().mockRejectedValue(makeError(429, 'Too many attempts', { remainingSeconds: 30 }))
  );
  await submitForm();
  await waitFor(() =>
    expect(
      screen.getByText('Account temporarily locked due to too many failed attempts.')
    ).toBeInTheDocument()
  );
  expect(screen.getByText(/try again in/i)).toBeInTheDocument();
});

test('calls login with correct credentials on successful submit', async () => {
  const loginFn = jest.fn().mockResolvedValue({ userId: '1', email: 'test@example.com', role: 'USER' });
  renderLogin(loginFn);
  await submitForm('test@example.com', 'mypassword');
  await waitFor(() =>
    expect(loginFn).toHaveBeenCalledWith('test@example.com', 'mypassword', '')
  );
});

test('disables inputs and button during lockout', async () => {
  renderLogin(
    jest.fn().mockRejectedValue(makeError(429, 'Too many attempts', { remainingSeconds: 30 }))
  );
  await submitForm();
  await waitFor(() =>
    expect(screen.getByLabelText(/email/i)).toBeDisabled()
  );
  expect(screen.getByLabelText(/password/i)).toBeDisabled();
  expect(screen.getByRole('button', { name: /account locked/i })).toBeDisabled();
});
