package com.example.booking.service;

import com.example.booking.dto.AppointmentRequest;
import com.example.booking.dto.UserAppointmentsResponse;
import com.example.booking.model.Appointment;
import com.example.booking.model.User;

import java.util.List;
import java.util.Optional;

public interface AppointmentService {
    List<Appointment> getAllAppointments();

    List<Appointment> getAppointmentsForUser(Long userId);

    UserAppointmentsResponse getUpcomingAndHistoryForUser(Long userId);

    Optional<Appointment> getAppointment(Long id);

    Appointment bookAppointment(Appointment appointment, Long timeSlotId, User user);

    Appointment bookAppointmentForUser(String username, AppointmentRequest request);

    void cancelAppointment(Long id);

    boolean cancelAppointmentByToken(String token);
}
