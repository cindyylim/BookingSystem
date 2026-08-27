import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TimeSlotAdmin from './TimeSlotAdmin';

beforeEach(() => {
  global.fetch = jest.fn();
});

afterEach(() => {
  jest.resetAllMocks();
});

test('creates a time slot', async () => {
  global.fetch.mockImplementation((url, options) => {
    if (url === '/api/timeslots' && options && options.method === 'POST') {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ id: 9 }) });
    }
    if (url === '/api/timeslots') {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
  });

  render(<TimeSlotAdmin />);
  await screen.findByText(/Manage Time Slots/i);
  fireEvent.change(screen.getByLabelText(/Start Time/i), { target: { value: '2024-06-01T10:00' } });
  fireEvent.change(screen.getByLabelText(/End Time/i), { target: { value: '2024-06-01T11:00' } });
  await userEvent.click(screen.getByRole('button', { name: /^Create$/i }));

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith('/api/timeslots', expect.objectContaining({ method: 'POST' }));
  });
});

test('shows error when create overlaps with 400', async () => {
  global.fetch.mockImplementation((url, options) => {
    if (url === '/api/timeslots' && options && options.method === 'POST') {
      return Promise.resolve({
        ok: false,
        status: 400,
        json: () => Promise.resolve({ message: 'Time slot overlaps with an existing slot.' }),
      });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
  });

  render(<TimeSlotAdmin />);
  await screen.findByText(/Manage Time Slots/i);
  fireEvent.change(screen.getByLabelText(/Start Time/i), { target: { value: '2024-06-01T10:00' } });
  fireEvent.change(screen.getByLabelText(/End Time/i), { target: { value: '2024-06-01T11:00' } });
  await userEvent.click(screen.getByRole('button', { name: /^Create$/i }));
  expect(await screen.findByText(/Failed to create time slot/i)).toBeInTheDocument();
});

test('booked slot has no delete button', async () => {
  global.fetch.mockResolvedValue({
    ok: true,
    json: () => Promise.resolve([
      {
        id: 8,
        startTime: '2024-06-01T15:00:00Z',
        endTime: '2024-06-01T16:00:00Z',
        available: false,
        appointments: [{ clientName: 'Pat', clientEmail: 'p@x.com', clientPhone: '1', service: 'Cut', location: 'A' }],
      },
    ]),
  });

  render(<TimeSlotAdmin />);
  expect(await screen.findByText(/^Booked$/i)).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: /^Delete$/i })).not.toBeInTheDocument();
});

test('delete of booked slot with 409 still refreshes list', async () => {
  let deleted = false;
  global.fetch.mockImplementation((url, options) => {
    if (url === '/api/timeslots/3' && options && options.method === 'DELETE') {
      deleted = true;
      return Promise.resolve({ ok: false, status: 409, json: () => Promise.resolve({ message: 'booked' }) });
    }
    if (url === '/api/timeslots') {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([
          {
            id: 3,
            startTime: '2024-06-01T15:00:00Z',
            endTime: '2024-06-01T16:00:00Z',
            available: true,
            appointments: deleted ? [{ clientName: 'Pat' }] : [],
          },
        ]),
      });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
  });

  render(<TimeSlotAdmin />);
  const deleteBtn = await screen.findByRole('button', { name: /^Delete$/i });
  await userEvent.click(deleteBtn);
  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith('/api/timeslots/3', { method: 'DELETE' });
  });
});

test('loads slots and deletes an available slot', async () => {
  global.fetch.mockImplementation((url, options) => {
    if (url === '/api/timeslots' && (!options || !options.method)) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([
          {
            id: 3,
            startTime: '2024-06-01T15:00:00Z',
            endTime: '2024-06-01T16:00:00Z',
            available: true,
            appointments: [],
          },
        ]),
      });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
  });

  render(<TimeSlotAdmin />);
  expect(await screen.findByText(/Manage Time Slots/i)).toBeInTheDocument();
  const deleteBtn = await screen.findByRole('button', { name: /^Delete$/i });
  await userEvent.click(deleteBtn);
  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith('/api/timeslots/3', { method: 'DELETE' });
  });
});
