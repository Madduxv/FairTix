import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Dashboard from './Dashboard';
import { AuthContext } from '../auth/AuthContext';
import api from '../api/client';

jest.mock('../api/client', () => ({
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
}));

const user = {
  userId: 'u1',
  email: 'test@example.com',
  role: 'USER',
  emailVerified: true,
};

const unverifiedUser = { ...user, emailVerified: false };

function renderDashboard(currentUser = user) {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ user: currentUser, logout: jest.fn(), login: jest.fn(), isLoading: false, sessionExpired: false, clearSessionExpired: jest.fn(), refreshUser: jest.fn() }}>
        <Dashboard />
      </AuthContext.Provider>
    </MemoryRouter>
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  api.get.mockResolvedValue({ emailOrder: true, emailTicket: true, emailHold: false, emailMarketing: false });
});

test('renders user info (email, role)', async () => {
  renderDashboard();
  await waitFor(() => expect(screen.getByText('test@example.com')).toBeInTheDocument());
  expect(screen.getByText('USER')).toBeInTheDocument();
});

test('shows email verification banner for unverified users', async () => {
  renderDashboard(unverifiedUser);
  await waitFor(() => expect(screen.getByText('Verify your email address')).toBeInTheDocument());
});

test('does not show verification banner for verified users', async () => {
  renderDashboard();
  await waitFor(() => expect(screen.getByText('test@example.com')).toBeInTheDocument());
  expect(screen.queryByText('Verify your email address')).not.toBeInTheDocument();
});

test('renders notification preferences with toggles', async () => {
  renderDashboard();
  await waitFor(() => expect(screen.getByText('Order confirmation emails')).toBeInTheDocument());
  expect(screen.getByText('Ticket issuance emails')).toBeInTheDocument();
});

test('saves notification preferences', async () => {
  api.put.mockResolvedValue({ emailOrder: false, emailTicket: true, emailHold: false, emailMarketing: false });
  renderDashboard();
  await waitFor(() => expect(screen.getByText('Order confirmation emails')).toBeInTheDocument());
  fireEvent.click(screen.getByRole('button', { name: /save preferences/i }));
  await waitFor(() => expect(screen.getByText('Preferences saved.')).toBeInTheDocument());
});

test('shows delete account dialog', async () => {
  renderDashboard();
  await waitFor(() => expect(screen.getByText('test@example.com')).toBeInTheDocument());
  fireEvent.click(screen.getByRole('button', { name: /delete my account/i }));
  expect(screen.getByText(/this action is/i)).toBeInTheDocument();
  expect(screen.getByPlaceholderText('Enter your email')).toBeInTheDocument();
});

test('delete account button disabled until email matches', async () => {
  renderDashboard();
  await waitFor(() => expect(screen.getByText('test@example.com')).toBeInTheDocument());
  fireEvent.click(screen.getByRole('button', { name: /delete my account/i }));
  const confirmBtn = screen.getAllByRole('button', { name: /delete my account/i }).pop();
  expect(confirmBtn).toBeDisabled();
  fireEvent.change(screen.getByPlaceholderText('Enter your email'), { target: { value: 'test@example.com' } });
  expect(confirmBtn).not.toBeDisabled();
});
