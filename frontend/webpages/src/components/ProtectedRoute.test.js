import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import { AuthContext } from '../auth/AuthContext';

function makeAuth(overrides = {}) {
  return {
    user: null,
    isLoading: false,
    sessionExpired: false,
    clearSessionExpired: jest.fn(),
    login: jest.fn(),
    logout: jest.fn(),
    refreshUser: jest.fn(),
    ...overrides,
  };
}

function renderProtected(authValue, initialPath = '/protected') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AuthContext.Provider value={authValue}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/protected" element={<div>Protected Content</div>} />
          </Route>
          <Route path="/login" element={<div>Login Page</div>} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>
  );
}

test('shows loading indicator while auth is initializing', () => {
  renderProtected(makeAuth({ isLoading: true }));
  expect(screen.getByText('Loading...')).toBeInTheDocument();
});

test('redirects to /login when user is not authenticated', () => {
  renderProtected(makeAuth({ user: null }));
  expect(screen.getByText('Login Page')).toBeInTheDocument();
  expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
});

test('renders outlet when user is authenticated', () => {
  renderProtected(makeAuth({ user: { userId: '1', email: 'a@b.com', role: 'USER' } }));
  expect(screen.getByText('Protected Content')).toBeInTheDocument();
  expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
});
