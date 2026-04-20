import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Navbar from './Navbar';
import { AuthContext } from '../auth/AuthContext';

function makeAuth(user = null, logout = jest.fn()) {
  return {
    user,
    isLoading: false,
    sessionExpired: false,
    clearSessionExpired: jest.fn(),
    login: jest.fn(),
    signup: jest.fn(),
    logout,
    refreshUser: jest.fn(),
  };
}

function renderNavbar(user = null, logout = jest.fn()) {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={makeAuth(user, logout)}>
        <Navbar />
      </AuthContext.Provider>
    </MemoryRouter>
  );
}

test('always shows Events link and FairTix brand', () => {
  renderNavbar();
  expect(screen.getByRole('link', { name: /fairtix/i })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /^events$/i })).toBeInTheDocument();
});

test('shows Log In and Sign Up links when not authenticated', () => {
  renderNavbar(null);
  expect(screen.getByRole('link', { name: /log in/i })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /sign up/i })).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: /log out/i })).not.toBeInTheDocument();
});

test('hides Log In and Sign Up when authenticated', () => {
  renderNavbar({ userId: '1', email: 'user@test.com', role: 'USER' });
  expect(screen.queryByRole('link', { name: /log in/i })).not.toBeInTheDocument();
  expect(screen.queryByRole('link', { name: /sign up/i })).not.toBeInTheDocument();
});

test('shows authenticated navigation links when logged in', () => {
  renderNavbar({ userId: '1', email: 'user@test.com', role: 'USER' });
  expect(screen.getByRole('link', { name: /my tickets/i })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /my holds/i })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /dashboard/i })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /log out/i })).toBeInTheDocument();
});

test('shows Admin Panel link only for ADMIN role', () => {
  renderNavbar({ userId: '1', email: 'admin@test.com', role: 'ADMIN' });
  expect(screen.getByRole('link', { name: /admin panel/i })).toBeInTheDocument();
});

test('does not show Admin Panel link for USER role', () => {
  renderNavbar({ userId: '1', email: 'user@test.com', role: 'USER' });
  expect(screen.queryByRole('link', { name: /admin panel/i })).not.toBeInTheDocument();
});

test('calls logout when Log Out button is clicked', () => {
  const logout = jest.fn();
  renderNavbar({ userId: '1', email: 'user@test.com', role: 'USER' }, logout);
  fireEvent.click(screen.getByRole('button', { name: /log out/i }));
  expect(logout).toHaveBeenCalledTimes(1);
});
