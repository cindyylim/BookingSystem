import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AppointmentForm from './AppointmentForm';

const slot = {
  id: 7,
  startTime: '2024-06-01T15:00:00Z',
  endTime: '2024-06-01T16:00:00Z',
};

const user = { username: 'pat', email: 'pat@example.com', phone: '5551112222' };

beforeEach(() => {
  global.fetch = jest.fn();
});

afterEach(() => {
  jest.resetAllMocks();
});

test('books appointment and calls onBooked', async () => {
  const onBooked = jest.fn();
  const booked = { id: 1, service: 'Haircut', location: 'Main St' };
  global.fetch.mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(booked),
  });

  render(<AppointmentForm timeSlot={slot} onBooked={onBooked} onCancel={jest.fn()} user={user} />);

  await userEvent.type(screen.getByLabelText(/Service Type/i), 'Haircut');
  await userEvent.type(screen.getByLabelText(/Preferred Location/i), 'Main St');
  await userEvent.click(screen.getByRole('button', { name: /Confirm Booking/i }));

  await waitFor(() => expect(onBooked).toHaveBeenCalledWith(booked));
  expect(global.fetch).toHaveBeenCalledWith('/api/appointments', expect.objectContaining({ method: 'POST' }));
});

test('calls onCancel', async () => {
  const onCancel = jest.fn();
  render(<AppointmentForm timeSlot={slot} onBooked={jest.fn()} onCancel={onCancel} user={user} />);
  await userEvent.click(screen.getByRole('button', { name: /^Cancel$/i }));
  expect(onCancel).toHaveBeenCalled();
});
