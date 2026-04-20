import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ResetPassword from './ResetPassword';
import api from '../api/client';

jest.mock('../api/client', () => ({
  post: jest.fn(),
}));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

function renderResetPassword() {
  return render(
    <MemoryRouter initialEntries={['/reset-password?token=valid-token']}>
      <ResetPassword />
    </MemoryRouter>
  );
}

beforeEach(() => jest.clearAllMocks());

test('renders password form when token is present', () => {
  renderResetPassword();
  expect(screen.getByLabelText('New Password')).toBeInTheDocument();
  expect(screen.getByLabelText('Confirm New Password')).toBeInTheDocument();
});

test('shows password requirement checklist when typing', async () => {
  renderResetPassword();
  fireEvent.change(screen.getByLabelText('New Password'), { target: { value: 'a' } });
  await waitFor(() => expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument());
  expect(screen.getByText(/uppercase letter/i)).toBeInTheDocument();
});

test('shows error when passwords do not match', async () => {
  renderResetPassword();
  fireEvent.change(screen.getByLabelText('New Password'), { target: { value: 'StrongP@ss1' } });
  fireEvent.change(screen.getByLabelText('Confirm New Password'), { target: { value: 'Different1!' } });
  fireEvent.click(screen.getByRole('button', { name: /reset password/i }));
  await waitFor(() => expect(screen.getByText('Passwords do not match.')).toBeInTheDocument());
});

test('navigates to login on successful reset', async () => {
  api.post.mockResolvedValue({});
  renderResetPassword();
  fireEvent.change(screen.getByLabelText('New Password'), { target: { value: 'StrongP@ss1' } });
  fireEvent.change(screen.getByLabelText('Confirm New Password'), { target: { value: 'StrongP@ss1' } });
  fireEvent.click(screen.getByRole('button', { name: /reset password/i }));
  await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/login', expect.anything()));
});

test('shows expired token message on 400 with expired message', async () => {
  const err = new Error('expired');
  err.status = 400;
  err.body = { message: 'Token expired' };
  api.post.mockRejectedValue(err);
  renderResetPassword();
  fireEvent.change(screen.getByLabelText('New Password'), { target: { value: 'StrongP@ss1' } });
  fireEvent.change(screen.getByLabelText('Confirm New Password'), { target: { value: 'StrongP@ss1' } });
  fireEvent.click(screen.getByRole('button', { name: /reset password/i }));
  await waitFor(() => expect(screen.getByText('Reset Link Expired')).toBeInTheDocument());
});

test('shows server error on 500', async () => {
  const err = new Error('server');
  err.status = 500;
  api.post.mockRejectedValue(err);
  renderResetPassword();
  fireEvent.change(screen.getByLabelText('New Password'), { target: { value: 'StrongP@ss1' } });
  fireEvent.change(screen.getByLabelText('Confirm New Password'), { target: { value: 'StrongP@ss1' } });
  fireEvent.click(screen.getByRole('button', { name: /reset password/i }));
  await waitFor(() => expect(screen.getByText(/server error/i)).toBeInTheDocument());
});
