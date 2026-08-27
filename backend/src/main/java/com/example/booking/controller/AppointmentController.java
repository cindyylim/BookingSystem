package com.example.booking.controller;

import com.example.booking.dto.AppointmentRequest;
import com.example.booking.model.Appointment;
import com.example.booking.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public List<Appointment> getAllAppointments(Authentication authentication) {
        return appointmentService.getAppointmentsVisibleTo(authentication.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getAppointment(@PathVariable Long id, Authentication authentication) {
        return appointmentService.getAppointmentForUser(id, authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Appointment> bookAppointment(@RequestBody AppointmentRequest request,
            Authentication authentication) {
        Appointment booked = appointmentService.bookAppointmentForUser(authentication.getName(), request);
        return ResponseEntity.ok(booked);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long id, Authentication authentication) {
        appointmentService.cancelAppointment(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/cancel/{token}", method = { RequestMethod.GET, RequestMethod.DELETE })
    public ResponseEntity<String> cancelAppointmentByToken(@PathVariable String token) {
        boolean cancelled = appointmentService.cancelAppointmentByToken(token);
        if (cancelled) {
            return ResponseEntity.ok("Your appointment has been cancelled.");
        }
        return ResponseEntity.status(404).body("Invalid or already cancelled appointment.");
    }
}
