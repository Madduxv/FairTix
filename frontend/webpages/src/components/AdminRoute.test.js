import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import AdminRoute from './AdminRoute';
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

function renderAdmin(authValue, initialPath = '/admin') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AuthContext.Provider value={authValue}>
        <Routes>
          <Route element={<AdminRoute />}>
            <Route path="/admin" element={<div>Admin Content</div>} />
          </Route>
          <Route path="/login" element={<div>Login Page</div>} />
          <Route path="/" element={<div>Home Page</div>} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>
  );
}

test('shows loading indicator while auth is initializing', () => {
  renderAdmin(makeAuth({ isLoading: true }));
  expect(screen.getByText('Loading...')).toBeInTheDocument();
});

test('redirects to /login when not authenticated', () => {
  renderAdmin(makeAuth({ user: null }));
  expect(screen.getByText('Login Page')).toBeInTheDocument();
  expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
});

test('redirects to / when user is not ADMIN', () => {
  renderAdmin(makeAuth({ user: { userId: '1', email: 'a@b.com', role: 'USER' } }));
  expect(screen.getByText('Home Page')).toBeInTheDocument();
  expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
});

test('renders outlet when user has ADMIN role', () => {
  renderAdmin(makeAuth({ user: { userId: '1', email: 'a@b.com', role: 'ADMIN' } }));
  expect(screen.getByText('Admin Content')).toBeInTheDocument();
  expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
});
