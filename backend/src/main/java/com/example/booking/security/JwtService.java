package com.example.booking.security;

public interface JwtService {
    String generateToken(String username);

    String extractUsername(String token);

    boolean validateToken(String token);
}
