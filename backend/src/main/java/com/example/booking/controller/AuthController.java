package com.example.booking.controller;

import com.example.booking.model.User;
import com.example.booking.service.UserService;
import com.example.booking.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userService.getUserByUsername(req.getUsername()).isPresent() ||
                userService.getUserByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username or email already exists"));
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());

        userService.createUser(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        return userService.getUserByUsername(req.getUsername()).map(user -> {
            if (passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                String token = jwtUtil.generateToken(user.getUsername());

                ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                        .httpOnly(true)
                        .path("/")
                        .maxAge(24 * 60 * 60) // 1 day
                        .build();

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                        .body(Map.of(
                                "message", "Login successful",
                                "user", Map.of(
                                        "username", user.getUsername(),
                                        "email", user.getEmail(),
                                        "phone", user.getPhone(),
                                        "role", user.getRole())));
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
            }
        }).orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid credentials")));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(Map.of("message", "Logout successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return userService.getUserByUsername(principal.getName()).map(user -> ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "email", user.getEmail(),
                "phone", user.getPhone(),
                "role", user.getRole()))).orElse(ResponseEntity.status(404).build());
    }

    public static class RegisterRequest {
        @NotBlank
        private String username;

        @NotBlank
        @Size(min = 6)
        private String password;

        @Email
        @NotBlank
        private String email;

        @NotBlank
        @Pattern(regexp = "^\\d{10}$")
        private String phone;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    // dto/LoginRequest.java
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
