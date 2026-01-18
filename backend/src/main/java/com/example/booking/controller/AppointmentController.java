package com.example.booking.controller;

import com.example.booking.model.Appointment;
import com.example.booking.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import com.example.booking.model.User;
import com.example.booking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;

    @Autowired
    private UserService userService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public List<Appointment> getAllAppointments() {
        return appointmentService.getAllAppointments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getAppointment(@PathVariable Long id) {
        Optional<Appointment> appointment = appointmentService.getAppointment(id);
        return appointment.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> bookAppointment(@RequestBody AppointmentRequest request,
            Authentication authentication) {
        try {
            // Check if user is authenticated
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(java.util.Map.of("message", "Authentication required to book appointments", "error", "Please login to book an appointment"));
            }
            
            // Get authenticated user
            User user = userService.getUserByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            Appointment appointment = new Appointment();
            // Use authenticated user's information
            appointment.setCustomerName(user.getUsername());
            appointment.setCustomerEmail(user.getEmail() != null ? user.getEmail() : request.getCustomerEmail());
            appointment.setCustomerPhone(user.getPhone() != null ? user.getPhone() : request.getCustomerPhone());
            appointment.setLocation(request.getLocation());
            appointment.setService(request.getService());
            
            Appointment booked = appointmentService.bookAppointment(appointment, request.getTimeSlotId(), user);
            return ResponseEntity.ok(booked);
        } catch (IllegalStateException e) {
            // Time slot is not available (race condition)
            return ResponseEntity.status(409)
                    .body(java.util.Map.of("message", "Time slot is not available", "error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            // Time slot not found or user not found
            return ResponseEntity.status(400)
                    .body(java.util.Map.of("message", e.getMessage(), "error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(java.util.Map.of("message", "Failed to book appointment", "error", e.getMessage()));
        }
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
        } else {
            return ResponseEntity.status(404).body("Invalid or already cancelled appointment.");
        }
    }

    // DTO for booking request
    public static class AppointmentRequest {
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private Long timeSlotId;
        private String location;
        private String service;

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getCustomerEmail() {
            return customerEmail;
        }

        public void setCustomerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
        }

        public String getCustomerPhone() {
            return customerPhone;
        }

        public void setCustomerPhone(String customerPhone) {
            this.customerPhone = customerPhone;
        }

        public Long getTimeSlotId() {
            return timeSlotId;
        }

        public void setTimeSlotId(Long timeSlotId) {
            this.timeSlotId = timeSlotId;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }
    }
}