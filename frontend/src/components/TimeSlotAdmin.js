// TimeSlotAdmin.js
// Admin interface to create and delete time slots.
import React, { useEffect, useState, useCallback } from 'react';
import {
  Card, CardContent, Typography, TextField, Button,
  List, ListItem, ListItemText, Box, Grid, Chip, Divider, Alert
} from '@mui/material';

function TimeSlotAdmin() {
  const [timeSlots, setTimeSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [error, setError] = useState('');

  const fetchTimeSlots = useCallback(() => {
    fetch('/api/timeslots')
      .then(res => res.json())
      .then(data => {
        setTimeSlots(data);
        setLoading(false);
      });

  }, []);

  useEffect(() => {
    fetchTimeSlots();
  }, [fetchTimeSlots]);

  const handleCreate = async (e) => {
    e.preventDefault();
    setError('');
    if (!startTime || !endTime) {
      setError('Start and end time are required.');
      return;
    }
    // Convert local datetime to UTC ISO string
    const startUtc = new Date(startTime).toISOString();
    const endUtc = new Date(endTime).toISOString();
    const res = await fetch('/api/timeslots', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        startTime: startUtc,
        endTime: endUtc,
        available: true
      })
    });
    if (res.ok) {
      setStartTime('');
      setEndTime('');
      fetchTimeSlots();
    } else {
      setError('Failed to create time slot.');
    }
  };

  const handleDelete = async (id) => {
    await fetch(`/api/timeslots/${id}`, {
      method: 'DELETE'
    });
    fetchTimeSlots();
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>Manage Time Slots</Typography>

      <Card elevation={3} sx={{ mb: 4 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>Create New Slot</Typography>
          <Box component="form" onSubmit={handleCreate}>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={5}>
                <TextField
                  label="Start Time"
                  type="datetime-local"
                  value={startTime}
                  onChange={e => setStartTime(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                  fullWidth
                  required
                />
              </Grid>
              <Grid item xs={12} sm={5}>
                <TextField
                  label="End Time"
                  type="datetime-local"
                  value={endTime}
                  onChange={e => setEndTime(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                  fullWidth
                  required
                />
              </Grid>
              <Grid item xs={12} sm={2}>
                <Button type="submit" variant="contained" color="primary" fullWidth sx={{ height: '56px' }}>
                  Create
                </Button>
              </Grid>
            </Grid>
            {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
          </Box>
        </CardContent>
      </Card>

      {loading ? <Typography>Loading time slots...</Typography> : (
        <Card elevation={3}>
          <CardContent>
            <Typography variant="h6" gutterBottom>Existing Slots</Typography>
            <List>
              {timeSlots.map((ts, idx) => (
                <div key={ts.id}>
                  {idx > 0 && <Divider component="li" />}
                  <ListItem
                    secondaryAction={
                      ts.available && (!ts.appointments || ts.appointments.length === 0) && (
                        <Button
                          variant="contained"
                          color="error"
                          onClick={() => handleDelete(ts.id)}
                        >
                          Delete
                        </Button>
                      )
                    }
                    alignItems="flex-start"
                  >
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Typography variant="subtitle1" component="span">
                            {new Date(ts.startTime).toLocaleString()} - {new Date(ts.endTime).toLocaleTimeString()}
                          </Typography>
                          <Chip
                            label={ts.available ? "Available" : "Booked"}
                            color={ts.available ? "success" : "default"}
                            size="small"
                            variant={ts.available ? "filled" : "outlined"}
                          />
                        </Box>
                      }
                      primaryTypographyProps={{ component: 'div' }}
                      secondary={
                        ts.appointments && ts.appointments.length > 0 ? (
                          <Box sx={{ mt: 1 }}>
                            {ts.appointments.map((appt, i) => (
                              <Box key={i} sx={{ p: 1, bgcolor: '#f0f4f8', borderRadius: 1, mt: 1 }}>
                                <Typography variant="body2" color="text.primary" fontWeight="600">
                                  {appt.clientName}
                                </Typography>
                                <Typography variant="caption" color="text.secondary" display="block">
                                  Email: {appt.clientEmail}
                                </Typography>
                                <Typography variant="caption" color="text.secondary" display="block">
                                  Phone: {appt.clientPhone}
                                </Typography>
                                <Typography variant="caption" color="text.secondary" display="block">
                                  Service: {appt.service} | Location: {appt.location}
                                </Typography>
                              </Box>
                            ))}
                          </Box>
                        ) : "No appointments"
                      }
                      secondaryTypographyProps={{ component: 'div' }}
                    />
                  </ListItem>
                </div>
              ))}
            </List>
          </CardContent>
        </Card>
      )}
    </Box>
  );
}

export default TimeSlotAdmin; 