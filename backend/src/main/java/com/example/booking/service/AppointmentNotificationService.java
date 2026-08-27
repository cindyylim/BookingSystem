package com.example.booking.service;

import com.example.booking.model.Appointment;

public interface AppointmentNotificationService {
    void sendAppointmentConfirmation(Appointment appointment);
}
