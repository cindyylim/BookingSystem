import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BookingSuccess from './BookingSuccess';

const appointment = {
  customerName: 'Pat',
  service: 'Haircut',
  location: 'Main St',
  startTime: '2024-06-01T15:00:00Z',
  endTime: '2024-06-01T16:00:00Z',
};

beforeEach(() => {
  global.URL.createObjectURL = jest.fn(() => 'blob:mock');
  global.URL.revokeObjectURL = jest.fn();
});

afterEach(() => {
  jest.restoreAllMocks();
});

test('shows confirmation details and goes back', async () => {
  const onBackToBooking = jest.fn();
  render(<BookingSuccess appointment={appointment} onBackToBooking={onBackToBooking} />);

  expect(screen.getByText(/Your booking is confirmed/i)).toBeInTheDocument();
  expect(screen.getByText(/Haircut/)).toBeInTheDocument();
  expect(screen.getByText(/Main St/)).toBeInTheDocument();

  await userEvent.click(screen.getByRole('button', { name: /Back to Booking/i }));
  expect(onBackToBooking).toHaveBeenCalled();
});

test('adds appointment to calendar', async () => {
  const click = jest.fn();
  const originalCreate = document.createElement.bind(document);
  jest.spyOn(document, 'createElement').mockImplementation((tag) => {
    const el = originalCreate(tag);
    if (tag === 'a') {
      el.click = click;
    }
    return el;
  });

  render(<BookingSuccess appointment={appointment} onBackToBooking={jest.fn()} />);
  await userEvent.click(screen.getByRole('button', { name: /Add to Calendar/i }));

  expect(global.URL.createObjectURL).toHaveBeenCalled();
  expect(click).toHaveBeenCalled();
  expect(global.URL.revokeObjectURL).toHaveBeenCalled();
});
