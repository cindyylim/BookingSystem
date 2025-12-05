package com.example.booking.controller;

import com.example.booking.model.Appointment;
import com.example.booking.model.User;
import com.example.booking.repository.AppointmentRepository;
import com.example.booking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private AppointmentRepository appointmentRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        Optional<User> optionalUser = userService.getUserByUsername(auth.getName());
        if (optionalUser.isEmpty())
            return ResponseEntity.status(404).body("User not found");
        User user = optionalUser.get();
        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "email", user.getEmail(),
                "phone", user.getPhone()));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Authentication auth, @RequestBody Map<String, String> req) {
        Optional<User> optionalUser = userService.getUserByUsername(auth.getName());
        if (optionalUser.isEmpty())
            return ResponseEntity.status(404).body("User not found");
        User user = optionalUser.get();
        user.setEmail(req.getOrDefault("email", user.getEmail()));
        user.setPhone(req.getOrDefault("phone", user.getPhone()));
        userService.updateUser(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/appointments")
    public ResponseEntity<?> getUserAppointments(Authentication auth) {
        Optional<User> optionalUser = userService.getUserByUsername(auth.getName());
        if (optionalUser.isEmpty())
            return ResponseEntity.status(404).body("User not found");
        User user = optionalUser.get();
        List<Appointment> all = appointmentRepository.findByUserId(user.getId());
        if (all.isEmpty())
            return ResponseEntity.ok(Map.of("upcoming", List.of(), "history", List.of()));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<Appointment> upcoming = all.stream().filter(a -> a.getStartTime().isAfter(now))
                .collect(Collectors.toList());
        List<Appointment> history = all.stream().filter(a -> a.getStartTime().isBefore(now))
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "upcoming", upcoming,
                "history", history));
    }
}