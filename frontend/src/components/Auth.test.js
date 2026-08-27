import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Auth from './Auth';

beforeEach(() => {
  global.fetch = jest.fn();
  window.alert = jest.fn();
});

afterEach(() => {
  jest.resetAllMocks();
});

test('logs in and calls onAuth with user', async () => {
  const onAuth = jest.fn();
  global.fetch.mockResolvedValue({
    ok: true,
    json: () => Promise.resolve({ user: { username: 'pat', role: 'USER' } }),
  });

  render(<Auth onAuth={onAuth} />);
  await userEvent.type(screen.getByLabelText(/Username/i), 'pat');
  await userEvent.type(screen.getByLabelText(/Password/i), 'secret1');
  await userEvent.click(screen.getByRole('button', { name: /^Login$/i }));

  await waitFor(() => expect(onAuth).toHaveBeenCalledWith({ username: 'pat', role: 'USER' }));
  expect(global.fetch).toHaveBeenCalledWith('/api/auth/login', expect.objectContaining({ method: 'POST' }));
});

test('shows error on failed login', async () => {
  global.fetch.mockResolvedValue({ ok: false });
  render(<Auth onAuth={jest.fn()} />);
  await userEvent.type(screen.getByLabelText(/Username/i), 'pat');
  await userEvent.type(screen.getByLabelText(/Password/i), 'bad');
  await userEvent.click(screen.getByRole('button', { name: /^Login$/i }));
  expect(await screen.findByText(/Invalid credentials/i)).toBeInTheDocument();
});

test('registers then prompts login', async () => {
  global.fetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({}) });
  render(<Auth onAuth={jest.fn()} />);
  await userEvent.click(screen.getByRole('button', { name: /Don't have an account\? Register/i }));
  await userEvent.type(screen.getByLabelText(/Username/i), 'newuser');
  await userEvent.type(screen.getByLabelText(/Password/i), 'secret1');
  await userEvent.type(screen.getByLabelText(/Email Address/i), 'new@example.com');
  await userEvent.type(screen.getByLabelText(/Phone Number/i), '1234567890');
  await userEvent.click(screen.getByRole('button', { name: /^Register$/i }));

  await waitFor(() => expect(window.alert).toHaveBeenCalled());
  expect(global.fetch).toHaveBeenCalledWith('/api/auth/register', expect.objectContaining({ method: 'POST' }));
  expect(await screen.findByRole('button', { name: /^Login$/i })).toBeInTheDocument();
});
