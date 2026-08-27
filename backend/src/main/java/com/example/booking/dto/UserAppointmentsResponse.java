package com.example.booking.dto;

import com.example.booking.model.Appointment;

import java.util.List;

public class UserAppointmentsResponse {
    private final List<Appointment> upcoming;
    private final List<Appointment> history;

    public UserAppointmentsResponse(List<Appointment> upcoming, List<Appointment> history) {
        this.upcoming = upcoming;
        this.history = history;
    }

    public List<Appointment> getUpcoming() {
        return upcoming;
    }

    public List<Appointment> getHistory() {
        return history;
    }
}
