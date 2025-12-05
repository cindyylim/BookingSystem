// BookingSuccess.js
import React from 'react';

function formatDateTime(start, end) {
  const startDate = new Date(start);
  const endDate = new Date(end);
  const options = { timeZoneName: 'short' };
  return `${startDate.toLocaleString(undefined, options)} - ${endDate.toLocaleString(undefined, options)}`;
}

function generateICS(appointment) {
  const start = new Date(appointment.startTime).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z');
  const end = new Date(appointment.endTime).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z');
  return `BEGIN:VCALENDAR\nVERSION:2.0\nBEGIN:VEVENT\nSUMMARY:${appointment.service || 'Appointment'}\nLOCATION:${appointment.location || ''}\nDTSTART:${start}\nDTEND:${end}\nDESCRIPTION:Appointment with ${appointment.customerName}\nEND:VEVENT\nEND:VCALENDAR`;
}

const BookingSuccess = ({ appointment, onBackToBooking }) => {
  const handleAddToCalendar = () => {
    const icsContent = generateICS(appointment);
    const blob = new Blob([icsContent], { type: 'text/calendar' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'appointment.ics';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div style={{ textAlign: 'center', padding: 30 }}>
      <h2>Success! Your booking is confirmed.</h2>
      <div style={{ margin: '20px 0' }}>
        <strong>Date & Time:</strong> {formatDateTime(appointment.startTime, appointment.endTime)}<br />
        <strong>Service:</strong> {appointment.service}<br />
        <strong>Location:</strong> {appointment.location}
      </div>
      <button onClick={handleAddToCalendar}>Add to Calendar</button>
      <div style={{ marginTop: 20 }}>
        <button onClick={onBackToBooking}>Back to Booking</button>
      </div>
    </div>
  );
};

export default BookingSuccess; 