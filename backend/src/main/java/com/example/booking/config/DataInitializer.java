package com.example.booking.config;

import com.example.booking.model.User;
import com.example.booking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) throws Exception {
        // Create demo admin user if it doesn't exist
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@booking.com");
            admin.setPhone("1234567890");
            admin.setRole("ADMIN");

            userRepository.save(admin);
            System.out.println("✓ Demo admin user created successfully!");
            System.out.println("  Username: admin");
            System.out.println("  Password: admin123");
        } else {
            System.out.println("✓ Demo admin user already exists");
        }
    }
}
