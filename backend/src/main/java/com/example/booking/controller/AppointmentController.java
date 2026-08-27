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
    public List<Appointment> getAllAppointments() {
        return appointmentService.getAllAppointments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getAppointment(@PathVariable Long id) {
        return appointmentService.getAppointment(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Appointment> bookAppointment(@RequestBody AppointmentRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        Appointment booked = appointmentService.bookAppointmentForUser(authentication.getName(), request);
        return ResponseEntity.ok(booked);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
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
