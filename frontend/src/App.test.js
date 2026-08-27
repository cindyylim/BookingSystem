import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

jest.mock('./components/TimeSlotList', () => () => <div>Time slots</div>);

import App from './App';

beforeEach(() => {
  global.fetch = jest.fn((url) => {
    if (url === '/api/auth/me') {
      return Promise.resolve({ ok: false, json: () => Promise.resolve({}) });
    }
    if (url === '/api/timeslots') {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
  });
});

afterEach(() => {
  jest.resetAllMocks();
});

test('renders salon booking landing page', async () => {
  render(<App />);
  expect(screen.getByText(/Salon Booking/i)).toBeInTheDocument();
  expect(await screen.findByText(/Effortless Salon Scheduling/i)).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /Login \/ Register/i })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /Admin/i })).toBeInTheDocument();
});

test('opens auth when Login / Register is clicked', async () => {
  render(<App />);
  await screen.findByText(/Effortless Salon Scheduling/i);
  await userEvent.click(screen.getByRole('button', { name: /Login \/ Register/i }));
  expect(await screen.findByText(/Welcome Back/i)).toBeInTheDocument();
});

test('opens admin login panel', async () => {
  render(<App />);
  await userEvent.click(screen.getByRole('button', { name: /^Admin$/i }));
  expect(await screen.findByText(/Admin Login/i)).toBeInTheDocument();
});

test('restores admin session without fetching user bookings', async () => {
  global.fetch = jest.fn((url) => {
    if (url === '/api/auth/me') {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ username: 'admin', role: 'ADMIN' }),
      });
    }
    if (url === '/api/timeslots') {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
  });

  render(<App />);
  expect(await screen.findByText(/Admin Dashboard/i)).toBeInTheDocument();
  expect(global.fetch).not.toHaveBeenCalledWith('/api/user/appointments');
});

test('admin login shows dashboard and logout posts to auth', async () => {
  global.fetch = jest.fn((url, options) => {
    if (url === '/api/auth/me') {
      return Promise.resolve({ ok: false, json: () => Promise.resolve({}) });
    }
    if (url === '/api/auth/login' && options && options.method === 'POST') {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ user: { username: 'admin', role: 'ADMIN' } }),
      });
    }
    if (url === '/api/auth/logout') {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    }
    if (url === '/api/timeslots') {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
  });

  render(<App />);
  await userEvent.click(screen.getByRole('button', { name: /^Admin$/i }));
  await userEvent.click(await screen.findByRole('button', { name: /Auto-fill Demo Credentials/i }));
  await userEvent.click(screen.getByRole('button', { name: /^Login$/i }));
  expect(await screen.findByText(/Admin Dashboard/i)).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: /^Logout$/i }));
  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith('/api/auth/logout', { method: 'POST' });
  });
});

test('user logout, profile PUT, and cancel via token', async () => {
  global.fetch = jest.fn((url, options) => {
    if (url === '/api/auth/me') {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ username: 'pat', email: 'pat@example.com', phone: '1234567890', role: 'USER' }),
      });
    }
    if (url === '/api/user/appointments') {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          upcoming: [{
            id: 1,
            startTime: '2024-06-01T15:00:00Z',
            endTime: '2024-06-01T16:00:00Z',
            service: 'Haircut',
            location: 'Main',
            cancellationToken: 'tok-99',
          }],
          history: [],
        }),
      });
    }
    if (url === '/api/user/profile' && options && options.method === 'PUT') {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    }
    if (url === '/api/appointments/cancel/tok-99') {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    }
    if (url === '/api/auth/logout') {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    }
    if (url === '/api/timeslots') {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
  });

  render(<App />);
  expect(await screen.findByText(/Welcome, pat/i)).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: /My Dashboard/i }));
  expect(await screen.findByRole('heading', { name: /Upcoming Bookings/i })).toBeInTheDocument();

  fireEvent.submit(screen.getByDisplayValue('pat@example.com').closest('form'));
  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith('/api/user/profile', expect.objectContaining({ method: 'PUT' }));
  });

  await userEvent.click(screen.getByRole('button', { name: /^Cancel$/i }));
  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith('/api/appointments/cancel/tok-99', { method: 'DELETE' });
  });

  await userEvent.click(screen.getByRole('button', { name: /^Logout$/i }));
  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith('/api/auth/logout', { method: 'POST' });
  });
});

test('restores session and shows dashboard for a regular user', async () => {
  global.fetch = jest.fn((url) => {
    if (url === '/api/auth/me') {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ username: 'pat', email: 'pat@example.com', phone: '1234567890', role: 'USER' }),
      });
    }
    if (url === '/api/user/appointments') {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ upcoming: [], history: [] }),
      });
    }
    if (url === '/api/timeslots') {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
  });

  render(<App />);
  expect(await screen.findByText(/Welcome, pat/i)).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: /My Dashboard/i }));
  expect(await screen.findByRole('heading', { name: /Upcoming Bookings/i })).toBeInTheDocument();
});
