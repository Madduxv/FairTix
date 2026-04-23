import React from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthContext } from './auth/AuthContext';

export function mockUser(overrides = {}) {
  return {
    userId: 'user-123',
    email: 'test@example.com',
    role: 'USER',
    emailVerified: true,
    ...overrides,
  };
}

export function mockEvent(overrides = {}) {
  return {
    id: 'event-123',
    title: 'Test Event',
    venue: { name: 'Test Venue' },
    startTime: '2026-12-01T18:00:00Z',
    status: 'ACTIVE',
    ...overrides,
  };
}

export function mockTicket(overrides = {}) {
  return {
    id: 'ticket-123',
    eventId: 'event-123',
    eventTitle: 'Test Event',
    eventVenue: 'Test Venue',
    eventStartTime: '2026-12-01T18:00:00Z',
    status: 'VALID',
    seatSection: 'A',
    seatRow: '1',
    seatNumber: '5',
    price: 50.00,
    ...overrides,
  };
}

export function mockOrder(overrides = {}) {
  return {
    id: 'order-123',
    status: 'COMPLETED',
    totalAmount: 50.00,
    ...overrides,
  };
}

export function mockAuthContext(overrides = {}) {
  return {
    user: null,
    isLoading: false,
    sessionExpired: false,
    clearSessionExpired: jest.fn(),
    login: jest.fn(),
    signup: jest.fn(),
    logout: jest.fn(),
    refreshUser: jest.fn(),
    ...overrides,
  };
}

export function renderWithProviders(ui, { authValue, initialEntries = ['/'] } = {}) {
  const auth = authValue || mockAuthContext();
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <AuthContext.Provider value={auth}>
        {ui}
      </AuthContext.Provider>
    </MemoryRouter>
  );
}
