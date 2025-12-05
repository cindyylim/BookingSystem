// TimeSlotList.js
// Lists all available time slots and allows the user to book one, now in a calendar view.
import React, { useEffect, useState, useMemo } from 'react';
import { Calendar, Views, dateFnsLocalizer } from 'react-big-calendar';
import format from 'date-fns/format';
import parse from 'date-fns/parse';
import startOfWeek from 'date-fns/startOfWeek';
import getDay from 'date-fns/getDay';
import enUS from 'date-fns/locale/en-US';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import ButtonGroup from '@mui/material/ButtonGroup';
import Paper from '@mui/material/Paper';
import Divider from '@mui/material/Divider';
import ArrowBackIosNewIcon from '@mui/icons-material/ArrowBackIosNew';
import ArrowForwardIosIcon from '@mui/icons-material/ArrowForwardIos';

const locales = {
  'en-US': enUS,
};
const localizer = dateFnsLocalizer({
  format,
  parse,
  startOfWeek: () => startOfWeek(new Date(), { weekStartsOn: 0 }),
  getDay,
  locales,
});

function TimeSlotList({ onBook }) {
  const [timeSlots, setTimeSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [view, setView] = useState(Views.WEEK);
  const [date, setDate] = useState(new Date());

  useEffect(() => {
    const fetchTimeSlots = () => {
      fetch('/api/timeslots')
        .then(res => res.json())
        .then(data => {
          setTimeSlots(data);
          setLoading(false);
        })
        .catch(err => {
          console.error('Failed to fetch time slots:', err);
          setLoading(false);
        });
    };

    // Initial fetch
    fetchTimeSlots();

    // Auto-refresh every 30 seconds to keep data fresh
    const interval = setInterval(fetchTimeSlots, 30000);

    return () => clearInterval(interval);
  }, []);

  const events = useMemo(() =>
    timeSlots
      .filter(ts => ts.available)
      .map(ts => {
        const start = new Date(ts.startTime);
        const end = new Date(ts.endTime);

        return {
          id: ts.id,
          title: 'Book Now',
          start: start,
          end: end,
          resource: ts,
        };
      }),
    [timeSlots]
  );

  const handleSelectEvent = event => {
    onBook(event.resource);
  };

  if (loading) return <div>Loading time slots...</div>;

  return (
    <Card elevation={3} sx={{ mt: 4, mb: 4, borderRadius: 2 }}>
      <Box sx={{ p: 3, borderBottom: '1px solid #eee', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h5" fontWeight="700">Select an Appointment</Typography>
          <Typography variant="body2" color="text.secondary">
            Click on an available blue slot to proceed.
          </Typography>
        </Box>

        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <ButtonGroup variant="outlined" aria-label="outlined button group">
            <Button onClick={() => setView(Views.WEEK)} variant={view === Views.WEEK ? 'contained' : 'outlined'}>Week</Button>
            <Button onClick={() => setView(Views.DAY)} variant={view === Views.DAY ? 'contained' : 'outlined'}>Day</Button>
          </ButtonGroup>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Button size="small" onClick={() => setDate(new Date(date.setDate(date.getDate() - (view === Views.WEEK ? 7 : 1))))}>
              <ArrowBackIosNewIcon fontSize="small" />
            </Button>
            <Button size="small" onClick={() => setDate(new Date())}>Today</Button>
            <Button size="small" onClick={() => setDate(new Date(date.setDate(date.getDate() + (view === Views.WEEK ? 7 : 1))))}>
              <ArrowForwardIosIcon fontSize="small" />
            </Button>
          </Box>
        </Box>
      </Box>

      <CardContent sx={{ p: 0 }}>
        <Box sx={{ height: 600, p: 2, background: '#fff' }}>
          <Calendar
            localizer={localizer}
            events={events}
            startAccessor="start"
            endAccessor="end"
            views={{ week: true, day: true }}
            view={view}
            date={date}
            onView={setView}
            onNavigate={setDate}
            onSelectEvent={handleSelectEvent}
            selectable={false}
            popup
            toolbar={false} // Custom toolbar above
            defaultView={Views.WEEK}
            components={{
              event: ({ event }) => (
                <Box sx={{ p: 0.5, height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                  <Typography variant="caption" sx={{ fontWeight: 600, lineHeight: 1.2 }}>
                    {new Date(event.start).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 700 }}>
                    {event.title}
                  </Typography>
                </Box>
              )
            }}
            eventPropGetter={(event) => ({
              style: {
                backgroundColor: '#1976d2',
                borderRadius: '4px',
                opacity: 0.9,
                color: 'white',
                border: '0px',
                display: 'block'
              }
            })}
          />
        </Box>
      </CardContent>
    </Card>
  );
}

export default TimeSlotList; 