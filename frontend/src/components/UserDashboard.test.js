import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import UserDashboard from './UserDashboard';

const user = { username: 'pat', email: 'pat@example.com', phone: '5551112222' };

test('renders empty bookings and profile', () => {
  render(
    <UserDashboard
      user={user}
      bookings={{ upcoming: [], history: [] }}
      onCancel={jest.fn()}
      onProfileUpdate={jest.fn()}
    />
  );
  expect(screen.getByText(/Welcome, pat/i)).toBeInTheDocument();
  expect(screen.getByText(/No upcoming bookings/i)).toBeInTheDocument();
  expect(screen.getByText(/No past bookings/i)).toBeInTheDocument();
});

test('lists history and saves profile', async () => {
  const onProfileUpdate = jest.fn((e) => e.preventDefault());
  const history = {
    id: 4,
    startTime: '2024-01-01T15:00:00Z',
    endTime: '2024-01-01T16:00:00Z',
    service: 'Color',
    location: 'Downtown',
  };
  render(
    <UserDashboard
      user={user}
      bookings={{ upcoming: [], history: [history] }}
      onCancel={jest.fn()}
      onProfileUpdate={onProfileUpdate}
    />
  );
  expect(screen.getByText(/Color @ Downtown/i)).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: /^Update$/i }));
  expect(onProfileUpdate).toHaveBeenCalled();
});

test('cancels an upcoming booking', async () => {
  const onCancel = jest.fn();
  const upcoming = {
    id: 9,
    startTime: '2024-06-01T15:00:00Z',
    endTime: '2024-06-01T16:00:00Z',
    service: 'Haircut',
    location: 'Main',
    cancellationToken: 'tok',
  };
  render(
    <UserDashboard
      user={user}
      bookings={{ upcoming: [upcoming], history: [] }}
      onCancel={onCancel}
      onProfileUpdate={jest.fn()}
    />
  );
  await userEvent.click(screen.getByRole('button', { name: /^Cancel$/i }));
  expect(onCancel).toHaveBeenCalledWith(upcoming);
});
