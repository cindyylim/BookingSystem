// UserDashboard.js
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';

const UserDashboard = ({ user, bookings, onCancel, onProfileUpdate }) => {
  return (
    <Box sx={{ maxWidth: 700, mx: 'auto', mt: 4 }}>
      <Card elevation={3} sx={{ mb: 4 }}>
        <CardContent>
          <Typography variant="h5" sx={{ fontWeight: 600, mb: 2 }}>Welcome, {user.username}</Typography>
          <Typography variant="subtitle1" color="text.secondary">Profile</Typography>
          <form onSubmit={onProfileUpdate} style={{ marginTop: 12 }}>
            <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
              <input name="email" defaultValue={user.email} placeholder="Email" style={{ flex: 1, padding: 8, borderRadius: 6, border: '1px solid #ccc' }} />
              <input name="phone" defaultValue={user.phone} placeholder="Phone" style={{ flex: 1, padding: 8, borderRadius: 6, border: '1px solid #ccc' }} />
              <Button type="submit" variant="contained" color="primary">Update</Button>
            </Box>
          </form>
        </CardContent>
      </Card>
      <Card elevation={3} sx={{ mb: 4 }}>
        <CardContent>
          <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>Upcoming Bookings</Typography>
          <List>
            {bookings.upcoming.length === 0 && (
              <ListItem><ListItemText primary="No upcoming bookings." /></ListItem>
            )}
            {bookings.upcoming.map(b => (
              <ListItem key={b.id} sx={{ display: 'flex', alignItems: 'center' }} divider>
                <ListItemText
                  primary={`${new Date(b.startTime).toLocaleString()} - ${new Date(b.endTime).toLocaleTimeString()}`}
                  secondary={`${b.service} @ ${b.location}`}
                />
                <Button onClick={() => onCancel(b)} variant="contained" color="secondary">Cancel</Button>
              </ListItem>
            ))}
          </List>
        </CardContent>
      </Card>
      <Card elevation={3}>
        <CardContent>
          <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>Booking History</Typography>
          <List>
            {bookings.history.length === 0 && (
              <ListItem><ListItemText primary="No past bookings." /></ListItem>
            )}
            {bookings.history.map(b => (
              <ListItem key={b.id} divider>
                <ListItemText
                  primary={`${new Date(b.startTime).toLocaleString()} - ${new Date(b.endTime).toLocaleTimeString()}`}
                  secondary={`${b.service} @ ${b.location}`}
                />
              </ListItem>
            ))}
          </List>
        </CardContent>
      </Card>
    </Box>
  );
};

export default UserDashboard; 