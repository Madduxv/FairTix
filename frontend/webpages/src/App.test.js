import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import App from './App';
import * as tokenUtils from './auth/tokenUtils';

jest.mock('./auth/tokenUtils', () => ({
  fetchCurrentUser: jest.fn(),
}));

jest.mock('./api/client', () => ({
  __esModule: true,
  default: {
    get: jest.fn().mockResolvedValue(null),
    post: jest.fn().mockResolvedValue(null),
  },
}));

beforeEach(() => {
  tokenUtils.fetchCurrentUser.mockResolvedValue(null);
});

test('shows session expired banner when auth:session-expired event fires', async () => {
  render(<App />);
  act(() => window.dispatchEvent(new Event('auth:session-expired')));
  await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
  expect(screen.getByText(/session has expired/i)).toBeInTheDocument();
});

test('dismisses session expired banner when dismiss is clicked', async () => {
  render(<App />);
  act(() => window.dispatchEvent(new Event('auth:session-expired')));
  await waitFor(() => screen.getByRole('alert'));
  fireEvent.click(screen.getByRole('button', { name: /dismiss/i }));
  await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
});
