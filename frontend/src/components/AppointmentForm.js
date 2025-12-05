// AppointmentForm.js
// Form for booking an appointment for a selected time slot.
import React, { useState } from 'react';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid';
import EventAvailableIcon from '@mui/icons-material/EventAvailable';

function AppointmentForm({ timeSlot, onBooked, onCancel }) {
  const prev = timeSlot.previousAppointment || {};
  const [customerName, setCustomerName] = useState(prev.customerName || '');
  const [customerEmail, setCustomerEmail] = useState(prev.customerEmail || '');
  const [customerPhone, setCustomerPhone] = useState(prev.customerPhone || '');
  const [service, setService] = useState(prev.service || '');
  const [location, setLocation] = useState(prev.location || '');
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (timeSlot.previousAppointment && timeSlot.previousAppointment.cancellationToken) {
      await fetch(`/api/appointments/cancel/${timeSlot.previousAppointment.cancellationToken}`, { method: 'DELETE' });
    }
    const headers = { 'Content-Type': 'application/json' };

    const res = await fetch('/api/appointments', {
      method: 'POST',
      headers,
      body: JSON.stringify({
        customerName,
        customerEmail,
        customerPhone,
        timeSlotId: timeSlot.id,
        service,
        location
      })
    });
    if (res.ok) {
      const appointment = await res.json();
      onBooked(appointment);
    } else {
      setError('Appointment is not available.');
    }
  };

  return (
    <Card elevation={4} sx={{ maxWidth: 600, mx: 'auto', mt: 4, borderRadius: 2 }}>
      <Box sx={{ bgcolor: 'primary.main', color: 'primary.contrastText', p: 3, display: 'flex', alignItems: 'center' }}>
        <EventAvailableIcon sx={{ mr: 2, fontSize: 40 }} />
        <Box>
          <Typography variant="h5" fontWeight="700">Confirm Booking</Typography>
          <Typography variant="body2" sx={{ opacity: 0.9 }}>
            Please fill in your details below.
          </Typography>
        </Box>
      </Box>

      <CardContent sx={{ p: 4 }}>
        <Box sx={{ mb: 4, p: 2, bgcolor: 'background.default', borderRadius: 2, border: '1px solid #e0e0e0' }}>
          <Typography variant="subtitle2" color="text.secondary" gutterBottom>TIME SLOT DETAILS</Typography>
          <Typography variant="h6" color="primary.main">
            {new Date(timeSlot.startTime).toLocaleString(undefined, { dateStyle: 'full', timeStyle: 'short' })}
          </Typography>
          <Typography variant="body1">
            to {new Date(timeSlot.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </Typography>
        </Box>

        <Box component="form" onSubmit={handleSubmit}>
          <Typography variant="h6" gutterBottom sx={{ mb: 2 }}>Client Information</Typography>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                label="Full Name"
                value={customerName}
                onChange={e => setCustomerName(e.target.value)}
                required
                fullWidth
                variant="outlined"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Email Address"
                type="email"
                value={customerEmail}
                onChange={e => setCustomerEmail(e.target.value)}
                required
                fullWidth
                variant="outlined"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Phone Number"
                type="tel"
                value={customerPhone}
                onChange={e => setCustomerPhone(e.target.value)}
                required
                fullWidth
                variant="outlined"
              />
            </Grid>

            <Grid item xs={12} sx={{ mt: 1 }}>
              <Typography variant="h6" gutterBottom sx={{ mb: 2 }}>Service Details</Typography>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Service Type"
                value={service}
                onChange={e => setService(e.target.value)}
                required
                fullWidth
                placeholder="e.g. Haircut, Manicure"
                variant="outlined"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Preferred Location"
                value={location}
                onChange={e => setLocation(e.target.value)}
                required
                fullWidth
                placeholder="e.g. Downtown, Main St."
                variant="outlined"
              />
            </Grid>
          </Grid>

          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2, mt: 4, pt: 2, borderTop: '1px solid #eee' }}>
            <Button type="button" variant="outlined" color="inherit" onClick={onCancel}>
              Cancel
            </Button>
            <Button type="submit" variant="contained" color="primary" size="large" sx={{ px: 4 }}>
              Confirm Booking
            </Button>
          </Box>

          {error && (
            <Box sx={{ mt: 2, p: 2, bgcolor: '#ffebee', color: '#c62828', borderRadius: 1 }}>
              {error}
            </Box>
          )}
        </Box>
      </CardContent>
    </Card>
  );
}

export default AppointmentForm; 