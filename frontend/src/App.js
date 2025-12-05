import React, { useState, useEffect } from 'react';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import StarIcon from '@mui/icons-material/Star';

import BookingSuccess from './components/BookingSuccess';
import Auth from './components/Auth';
import UserDashboard from './components/UserDashboard';
import AdminLogin from './components/AdminLogin';
import TimeSlotAdmin from './components/TimeSlotAdmin';
import TimeSlotList from './components/TimeSlotList';
import AppointmentForm from './components/AppointmentForm';

const theme = createTheme({
  palette: {
    primary: { main: '#1976d2' },
    secondary: { main: '#f50057' },
    background: { default: '#f8fafc' },
  },
  shape: { borderRadius: 12 },
  components: {
    MuiButton: {
      styleOverrides: {
        root: { textTransform: 'none', fontWeight: 600 },
      },
    },
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h3: { fontWeight: 700 },
    h5: { fontWeight: 600 },
  },
});

function App() {
  const [selectedTimeSlot, setSelectedTimeSlot] = useState(null);
  const [refresh, setRefresh] = useState(false);
  const [bookingSuccess, setBookingSuccess] = useState(false);
  const [lastAppointment, setLastAppointment] = useState(null);
  const [user, setUser] = useState(null);
  const [showDashboard, setShowDashboard] = useState(false);
  const [userBookings, setUserBookings] = useState({ upcoming: [], history: [] });
  const [guestMode, setGuestMode] = useState(false);
  const [showAdminLogin, setShowAdminLogin] = useState(false);
  const [adminError, setAdminError] = useState(null);
  const [isAdmin, setIsAdmin] = useState(false);

  // Define fetchUserBookings early so it can be used in useEffect
  const fetchUserBookings = async () => {
    const res = await fetch('/api/user/appointments');
    if (res.ok) {
      const data = await res.json();
      setUserBookings(data);
    }
  };

  useEffect(() => {
    // Check for existing session
    fetch('/api/auth/me')
      .then(res => {
        if (res.ok) return res.json();
        throw new Error('Not authenticated');
      })
      .then(user => {
        setUser(user);
        // Fetch bookings for restored user session
        if (user && user.role !== 'ADMIN') {
          fetchUserBookings();
        }
      })
      .catch(() => {
        // Not logged in
        setUser(null);
      });
  }, []);

  // Fetch bookings when dashboard is shown
  useEffect(() => {
    if (showDashboard && user && user.role !== 'ADMIN') {
      fetchUserBookings();
    }
  }, [showDashboard, user]);

  const handleBook = (timeSlot) => {
    setSelectedTimeSlot(timeSlot);
  };

  const handleBooked = (appointment) => {
    setSelectedTimeSlot(null);
    setLastAppointment(appointment);
    setBookingSuccess(true);
    setRefresh(!refresh);
  };

  const handleCancel = () => {
    setSelectedTimeSlot(null);
  };

  const handleModify = () => {
    setBookingSuccess(false);
    setSelectedTimeSlot({
      ...lastAppointment.timeSlot,
      previousAppointment: lastAppointment
    });
  };

  const handleCancelBooking = async (appt) => {
    await fetch(`/api/appointments/cancel/${appt.cancellationToken}`, {
      method: 'DELETE'
    });
    fetchUserBookings();
  };

  const handleAuth = (userObj) => {
    setUser(userObj);
    setShowDashboard(true);
    fetchUserBookings();
  };

  const handleLogout = async () => {
    await fetch('/api/auth/logout', { method: 'POST' });
    setUser(null);
    setShowDashboard(false);
  };

  const handleProfileUpdate = async (e) => {
    e.preventDefault();
    const form = e.target;
    const email = form.email.value;
    const phone = form.phone.value;
    await fetch('/api/user/profile', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, phone })
    });
    fetchUserBookings();
  };

  const handleBackToBooking = () => {
    setBookingSuccess(false);
    setLastAppointment(null);
    setSelectedTimeSlot(null);
    setShowDashboard(false);
    setGuestMode(false);
  };

  const handleAdminLogin = (userObj) => {
    if (userObj.role === 'ADMIN') {
      setUser(userObj);
      setShowDashboard(false);
      setShowAdminLogin(false);
      setAdminError(null);
      setIsAdmin(true);
    } else {
      setAdminError('Access denied. Admin privileges required.');
    }
  };

  const handleAdminLogout = async () => {
    await fetch('/api/auth/logout', { method: 'POST' });
    setIsAdmin(false);
    setShowAdminLogin(false);
    setUser(null);
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AppBar position="static" color="primary" elevation={0} sx={{ borderBottom: '1px solid rgba(0,0,0,0.1)' }}>
        <Toolbar>
          <CalendarMonthIcon sx={{ mr: 2 }} />
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 700 }}>
            Salon Booking
          </Typography>
          <Button color="inherit" onClick={() => setShowAdminLogin(true)}>Admin</Button>
        </Toolbar>
      </AppBar>
      {showAdminLogin && (
        <Box sx={{ background: '#fff', border: '1px solid #ccc', p: 3, position: 'absolute', top: 70, right: 20, zIndex: 1200, borderRadius: 2, boxShadow: 3 }}>
          <AdminLogin onLogin={handleAdminLogin} error={adminError} />
          <Button onClick={() => setShowAdminLogin(false)} sx={{ mt: 2 }} fullWidth>Close</Button>
        </Box>
      )}

      {/* Hero Section */}
      {!user && !isAdmin && !bookingSuccess && !selectedTimeSlot && !showDashboard && !guestMode && (
        <Box sx={{
          background: 'linear-gradient(135deg, #1976d2 0%, #0d47a1 100%)',
          color: 'white',
          py: 8,
          textAlign: 'center',
          mb: 6
        }}>
          <Container maxWidth="md">
            <Typography variant="h3" sx={{ mb: 2 }}>
              Effortless Salon Scheduling
            </Typography>
            <Typography variant="h6" sx={{ mb: 4, opacity: 0.9 }}>
              Book appointments, manage your schedule, and grow your business with our modern platform.
            </Typography>
            <Button
              variant="contained"
              size="large"
              color="secondary"
              onClick={() => setGuestMode(true)}
              sx={{ borderRadius: 8, px: 6, py: 1.5, fontSize: '1.1rem', boxShadow: 3 }}
            >
              Book Now
            </Button>
          </Container>
        </Box>
      )}

      <Container maxWidth="lg" sx={{ mb: 8 }}>

        {/* Features Section (Only on landing) */}
        {!user && !isAdmin && !bookingSuccess && !selectedTimeSlot && !showDashboard && !guestMode && (
          <Grid container spacing={4} sx={{ mb: 6 }}>
            {[
              { icon: <CalendarMonthIcon fontSize="large" color="primary" />, title: "Easy Booking", desc: "Select a time slot that works for you in seconds." },
              { icon: <AccessTimeIcon fontSize="large" color="primary" />, title: "24/7 Access", desc: "Book your appointments anytime, anywhere." },
              { icon: <StarIcon fontSize="large" color="primary" />, title: "Premium Service", desc: "Experience the best salon services in town." }
            ].map((feature, idx) => (
              <Grid item xs={12} md={4} key={idx}>
                <Card elevation={2} sx={{ height: '100%', textAlign: 'center', p: 2 }}>
                  <CardContent>
                    <Box sx={{ mb: 2 }}>{feature.icon}</Box>
                    <Typography variant="h6" gutterBottom>{feature.title}</Typography>
                    <Typography variant="body2" color="text.secondary">{feature.desc}</Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}

        {/* Admin View */}
        {isAdmin && (
          <Box sx={{ mt: 4 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
              <Typography variant="h4" color="primary">Admin Dashboard</Typography>
              <Button onClick={handleAdminLogout} variant="outlined" color="error">Logout</Button>
            </Box>
            <TimeSlotAdmin />
          </Box>
        )}

        {/* User/Guest/Dashboard Logic */}
        {!isAdmin && (
          user ? (
            <Box sx={{ mb: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Typography variant="h5">Welcome, {user.username}</Typography>
              <Box>
                <Button onClick={() => setShowDashboard(!showDashboard)} variant="contained" sx={{ mr: 2 }}>
                  {showDashboard ? 'Book Appointment' : 'My Dashboard'}
                </Button>
                <Button onClick={handleLogout} variant="outlined" color="error">Logout</Button>
              </Box>
            </Box>
          ) : null
        )}

        {!isAdmin && showDashboard && user ? (
          <UserDashboard
            user={user}
            bookings={userBookings}
            onCancel={handleCancelBooking}
            onProfileUpdate={handleProfileUpdate}
          />
        ) : !user && !bookingSuccess && !selectedTimeSlot && !guestMode && !isAdmin ? (
          <Auth onAuth={handleAuth} />
        ) : bookingSuccess && lastAppointment ? (
          <BookingSuccess
            appointment={lastAppointment}
            onBackToBooking={handleBackToBooking}
          />
        ) : selectedTimeSlot ? (
          <AppointmentForm
            timeSlot={selectedTimeSlot}
            onBooked={appointment => handleBooked(appointment)}
            onCancel={handleCancel}
          />
        ) : (guestMode || user) && !isAdmin ? (
          <Box>
            {guestMode && !user && (
              <Button onClick={() => setGuestMode(false)} sx={{ mb: 2 }}>&larr; Back to Home</Button>
            )}
            <TimeSlotList onBook={handleBook} key={refresh} />
          </Box>
        ) : null}
      </Container>
    </ThemeProvider>
  );
}

export default App;
