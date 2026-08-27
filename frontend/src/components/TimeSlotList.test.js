import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TimeSlotList from './TimeSlotList';

jest.mock('date-fns/format', () => jest.fn(() => ''));
jest.mock('date-fns/parse', () => jest.fn());
jest.mock('date-fns/startOfWeek', () => jest.fn(() => new Date()));
jest.mock('date-fns/getDay', () => jest.fn(() => 0));
jest.mock('date-fns/locale/en-US', () => ({ code: 'en-US' }));
jest.mock('react-big-calendar', () => ({
  Calendar: ({ events, onSelectEvent }) => (
    <div>
      {events.length === 0 && <div>No available slots</div>}
      {events.map((event) => (
        <button key={event.id} type="button" onClick={() => onSelectEvent(event)}>
          {event.title}-{event.id}
        </button>
      ))}
    </div>
  ),
  Views: { WEEK: 'week', DAY: 'day' },
  dateFnsLocalizer: () => ({}),
}));

beforeEach(() => {
  global.fetch = jest.fn();
  jest.spyOn(console, 'error').mockImplementation(() => {});
});

afterEach(() => {
  jest.resetAllMocks();
  console.error.mockRestore();
});

test('loads available slots and books on click', async () => {
  const onBook = jest.fn();
  const slot = {
    id: 11,
    startTime: '2024-06-01T15:00:00Z',
    endTime: '2024-06-01T16:00:00Z',
    available: true,
  };
  global.fetch.mockResolvedValue({
    ok: true,
    json: () => Promise.resolve([slot, { ...slot, id: 12, available: false }]),
  });

  render(<TimeSlotList onBook={onBook} />);
  expect(screen.getByText(/Loading time slots/i)).toBeInTheDocument();
  expect(await screen.findByText(/Select an Appointment/i)).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: /Book Now-11/i }));
  expect(onBook).toHaveBeenCalledWith(slot);
});

test('shows calendar when there are no slots', async () => {
  global.fetch.mockResolvedValue({
    ok: true,
    json: () => Promise.resolve([]),
  });

  render(<TimeSlotList onBook={jest.fn()} />);
  expect(await screen.findByText(/Select an Appointment/i)).toBeInTheDocument();
  expect(screen.getByText(/No available slots/i)).toBeInTheDocument();
});

test('stops loading when fetch fails', async () => {
  global.fetch.mockRejectedValue(new Error('network down'));

  render(<TimeSlotList onBook={jest.fn()} />);
  expect(await screen.findByText(/Select an Appointment/i)).toBeInTheDocument();
  expect(console.error).toHaveBeenCalled();
});
