import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ForgotPassword from './ForgotPassword';
import api from '../api/client';

jest.mock('../api/client', () => ({
  post: jest.fn(),
}));

function renderForgotPassword() {
  return render(
    <MemoryRouter>
      <ForgotPassword />
    </MemoryRouter>
  );
}

beforeEach(() => jest.clearAllMocks());

test('renders form with email input and submit button', () => {
  renderForgotPassword();
  expect(screen.getByLabelText('Email')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /send reset link/i })).toBeInTheDocument();
});

test('shows success message after submission', async () => {
  api.post.mockResolvedValue({});
  renderForgotPassword();
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'test@example.com' } });
  fireEvent.click(screen.getByRole('button', { name: /send reset link/i }));
  await waitFor(() => expect(screen.getByText('Check your email')).toBeInTheDocument());
  expect(screen.getByText(/test@example.com/)).toBeInTheDocument();
});

test('shows rate limit error on 429', async () => {
  const err = new Error('rate limited');
  err.status = 429;
  api.post.mockRejectedValue(err);
  renderForgotPassword();
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'test@example.com' } });
  fireEvent.click(screen.getByRole('button', { name: /send reset link/i }));
  await waitFor(() => expect(screen.getByText(/too many requests/i)).toBeInTheDocument());
});

test('shows server error on 500', async () => {
  const err = new Error('server error');
  err.status = 500;
  api.post.mockRejectedValue(err);
  renderForgotPassword();
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'test@example.com' } });
  fireEvent.click(screen.getByRole('button', { name: /send reset link/i }));
  await waitFor(() => expect(screen.getByText(/server error/i)).toBeInTheDocument());
});

test('shows network error when no status', async () => {
  api.post.mockRejectedValue(new Error('fetch failed'));
  renderForgotPassword();
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'test@example.com' } });
  fireEvent.click(screen.getByRole('button', { name: /send reset link/i }));
  await waitFor(() => expect(screen.getByText(/network error/i)).toBeInTheDocument());
});
