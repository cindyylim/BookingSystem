package com.example.booking.controller;

import com.example.booking.dto.UserAppointmentsResponse;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.User;
import com.example.booking.service.AppointmentService;
import com.example.booking.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.example.booking.util.JsonMaps;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final AppointmentService appointmentService;

    public UserController(UserService userService, AppointmentService appointmentService) {
        this.userService = userService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        User user = requireUser(auth.getName());
        return ResponseEntity.ok(JsonMaps.ofNullable(
                "username", user.getUsername(),
                "email", user.getEmail(),
                "phone", user.getPhone()));
    }

    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(Authentication auth, @RequestBody Map<String, String> req) {
        userService.updateProfile(auth.getName(), req.get("email"), req.get("phone"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/appointments")
    public ResponseEntity<UserAppointmentsResponse> getUserAppointments(Authentication auth) {
        User user = requireUser(auth.getName());
        return ResponseEntity.ok(appointmentService.getUpcomingAndHistoryForUser(user.getId()));
    }

    private User requireUser(String username) {
        return userService.getUserByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
