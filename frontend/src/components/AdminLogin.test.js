import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AdminLogin from './AdminLogin';

beforeEach(() => {
  global.fetch = jest.fn();
});

afterEach(() => {
  jest.resetAllMocks();
});

test('logs in admin and calls onLogin', async () => {
  const onLogin = jest.fn();
  global.fetch.mockResolvedValue({
    ok: true,
    json: () => Promise.resolve({ user: { username: 'admin', role: 'ADMIN' } }),
  });

  render(<AdminLogin onLogin={onLogin} />);
  await userEvent.click(screen.getByRole('button', { name: /Auto-fill Demo Credentials/i }));
  await userEvent.click(screen.getByRole('button', { name: /^Login$/i }));

  await waitFor(() => expect(onLogin).toHaveBeenCalledWith({ username: 'admin', role: 'ADMIN' }));
  expect(global.fetch).toHaveBeenCalledWith('/api/auth/login', expect.objectContaining({ method: 'POST' }));
});

test('shows invalid credentials', async () => {
  global.fetch.mockResolvedValue({ ok: false });
  render(<AdminLogin onLogin={jest.fn()} />);
  await userEvent.type(screen.getByLabelText(/Username/i), 'admin');
  await userEvent.type(screen.getByLabelText(/Password/i), 'wrong');
  await userEvent.click(screen.getByRole('button', { name: /^Login$/i }));
  expect(await screen.findByText(/Invalid credentials/i)).toBeInTheDocument();
});

test('shows access denied from parent', () => {
  render(<AdminLogin onLogin={jest.fn()} error="Access denied. Admin privileges required." />);
  expect(screen.getByText(/Access denied/i)).toBeInTheDocument();
});
