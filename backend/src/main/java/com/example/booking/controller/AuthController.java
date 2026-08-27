package com.example.booking.controller;

import com.example.booking.dto.AuthResult;
import com.example.booking.dto.LoginRequest;
import com.example.booking.dto.RegisterRequest;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.User;
import com.example.booking.security.JwtService;
import com.example.booking.service.AuthService;
import com.example.booking.util.JsonMaps;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        AuthResult result = authService.login(req);
        User user = result.getUser();
        ResponseCookie jwtCookie = jwtCookie(result.getToken(), jwtService.getExpirationSeconds());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(Map.of(
                        "message", "Login successful",
                        "user", userView(user)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        ResponseCookie jwtCookie = jwtCookie("", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(Map.of("message", "Logout successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = authService.currentUser(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(userView(user));
    }

    private static ResponseCookie jwtCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private static Map<String, String> userView(User user) {
        return JsonMaps.ofNullable(
                "username", user.getUsername(),
                "email", user.getEmail(),
                "phone", user.getPhone(),
                "role", user.getRole());
    }
}
