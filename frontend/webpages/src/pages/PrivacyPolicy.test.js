import React from 'react';
import { render, screen } from '@testing-library/react';
import PrivacyPolicy from './PrivacyPolicy';

beforeEach(() => render(<PrivacyPolicy />));

test('renders all section headings', () => {
  expect(screen.getByText('Privacy Policy')).toBeInTheDocument();
  expect(screen.getByText('What We Collect')).toBeInTheDocument();
  expect(screen.getByText('How We Use Your Data')).toBeInTheDocument();
  expect(screen.getByText('Notification Preferences')).toBeInTheDocument();
  expect(screen.getByText('Data Retention & Deletion')).toBeInTheDocument();
  expect(screen.getByText('Data Export')).toBeInTheDocument();
  expect(screen.getByText('Cookies')).toBeInTheDocument();
  expect(screen.getByText('Contact')).toBeInTheDocument();
});

test('states that user data is never sold to third parties', () => {
  expect(screen.getByText(/never sell your personal data/i)).toBeInTheDocument();
});

test('states that cookies are HttpOnly and unreadable by JavaScript', () => {
  expect(screen.getByText(/http-only cookies/i)).toBeInTheDocument();
  expect(screen.getByText(/cannot be read by javascript/i)).toBeInTheDocument();
});

test('states that account deletion anonymizes personal data', () => {
  expect(screen.getByText(/personal data is anonymized/i)).toBeInTheDocument();
});

test('states that marketing emails are strictly opt-in', () => {
  expect(screen.getByText(/marketing emails are strictly opt-in/i)).toBeInTheDocument();
});
