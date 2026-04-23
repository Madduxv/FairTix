import React from 'react';
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider, AuthContext } from './AuthContext';
import * as tokenUtils from './tokenUtils';
import api from '../api/client';

jest.mock('./tokenUtils', () => ({
  fetchCurrentUser: jest.fn(),
}));

jest.mock('../api/client', () => ({
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  patch: jest.fn(),
  delete: jest.fn(),
}));

function AuthConsumer() {
  const auth = React.useContext(AuthContext);
  if (auth.isLoading) return <div>loading</div>;
  return (
    <div>
      <div data-testid="user-email">{auth.user ? auth.user.email : 'null'}</div>
      <div data-testid="session-expired">{String(auth.sessionExpired)}</div>
      <button onClick={() => auth.login('test@example.com', 'pass')}>do-login</button>
      <button onClick={() => auth.logout()}>do-logout</button>
      <button onClick={() => auth.clearSessionExpired()}>clear-expired</button>
    </div>
  );
}

function renderProvider() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>
    </MemoryRouter>
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  tokenUtils.fetchCurrentUser.mockResolvedValue(null);
  api.post.mockResolvedValue(null);
});

test('shows loading state on initial mount, then resolves to null user', async () => {
  renderProvider();
  expect(screen.getByText('loading')).toBeInTheDocument();
  await waitFor(() => expect(screen.getByTestId('user-email')).toBeInTheDocument());
  expect(screen.getByTestId('user-email')).toHaveTextContent('null');
});

test('hydrates user from fetchCurrentUser on mount', async () => {
  tokenUtils.fetchCurrentUser.mockResolvedValue({
    userId: 'u1',
    email: 'hydrated@test.com',
    role: 'USER',
    emailVerified: true,
  });
  renderProvider();
  await waitFor(() =>
    expect(screen.getByTestId('user-email')).toHaveTextContent('hydrated@test.com')
  );
});

test('login sets user on successful API response', async () => {
  api.post.mockResolvedValue({
    userId: 'u2',
    email: 'login@test.com',
    role: 'USER',
    emailVerified: true,
  });
  renderProvider();
  await waitFor(() => expect(screen.getByTestId('user-email')).toBeInTheDocument());

  await act(async () => {
    fireEvent.click(screen.getByText('do-login'));
  });

  await waitFor(() =>
    expect(screen.getByTestId('user-email')).toHaveTextContent('login@test.com')
  );
});

test('logout clears the authenticated user', async () => {
  tokenUtils.fetchCurrentUser.mockResolvedValue({
    userId: 'u1',
    email: 'user@test.com',
    role: 'USER',
    emailVerified: true,
  });
  renderProvider();
  await waitFor(() =>
    expect(screen.getByTestId('user-email')).toHaveTextContent('user@test.com')
  );

  await act(async () => {
    fireEvent.click(screen.getByText('do-logout'));
  });

  await waitFor(() =>
    expect(screen.getByTestId('user-email')).toHaveTextContent('null')
  );
});

test('auth:session-expired event sets sessionExpired to true', async () => {
  renderProvider();
  await waitFor(() => expect(screen.getByTestId('user-email')).toBeInTheDocument());

  await act(async () => {
    window.dispatchEvent(new CustomEvent('auth:session-expired'));
  });

  await waitFor(() =>
    expect(screen.getByTestId('session-expired')).toHaveTextContent('true')
  );
});

test('clearSessionExpired resets sessionExpired to false', async () => {
  renderProvider();
  await waitFor(() => expect(screen.getByTestId('user-email')).toBeInTheDocument());

  await act(async () => {
    window.dispatchEvent(new CustomEvent('auth:session-expired'));
  });
  await waitFor(() =>
    expect(screen.getByTestId('session-expired')).toHaveTextContent('true')
  );

  fireEvent.click(screen.getByText('clear-expired'));
  await waitFor(() =>
    expect(screen.getByTestId('session-expired')).toHaveTextContent('false')
  );
});
