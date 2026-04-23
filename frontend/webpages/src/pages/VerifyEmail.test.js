import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import VerifyEmail from './VerifyEmail';
import { AuthContext } from '../auth/AuthContext';

jest.mock('../api/client', () => ({
  get: jest.fn().mockResolvedValue({}),
}));

function renderVerifyEmail(searchParams = '?success=true', authOverrides = {}) {
  const auth = {
    user: { userId: 'u1', email: 'test@example.com', emailVerified: false },
    isLoading: false,
    refreshUser: jest.fn().mockResolvedValue({}),
    login: jest.fn(),
    logout: jest.fn(),
    sessionExpired: false,
    clearSessionExpired: jest.fn(),
    ...authOverrides,
  };
  return render(
    <MemoryRouter initialEntries={[`/verify-email${searchParams}`]}>
      <AuthContext.Provider value={auth}>
        <VerifyEmail />
      </AuthContext.Provider>
    </MemoryRouter>
  );
}

test('shows success message when success=true', async () => {
  renderVerifyEmail('?success=true');
  await waitFor(() => expect(screen.getByText('Email verified!')).toBeInTheDocument());
});

test('shows error message when error=true', async () => {
  renderVerifyEmail('?error=true');
  await waitFor(() => expect(screen.getByText('Verification failed')).toBeInTheDocument());
});

test('shows already verified when user email is verified', async () => {
  renderVerifyEmail('', { user: { userId: 'u1', email: 'test@example.com', emailVerified: true } });
  await waitFor(() => expect(screen.getByText('Already verified')).toBeInTheDocument());
});

test('shows verifying state while auth is loading', () => {
  renderVerifyEmail('?success=true', { isLoading: true });
  expect(screen.getByText('Verifying...')).toBeInTheDocument();
});
