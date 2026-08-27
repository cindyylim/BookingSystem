package com.example.booking.repository;

import com.example.booking.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsernameAndEmail() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("secret");
        user.setEmail("alice@example.com");
        user.setPhone("1234567890");
        user.setRole("USER");
        userRepository.save(user);

        Optional<User> byUsername = userRepository.findByUsername("alice");
        Optional<User> byEmail = userRepository.findByEmail("alice@example.com");

        assertTrue(byUsername.isPresent());
        assertEquals("alice@example.com", byUsername.get().getEmail());
        assertTrue(byEmail.isPresent());
        assertEquals("alice", byEmail.get().getUsername());
        assertTrue(userRepository.findByUsername("missing").isEmpty());
        assertTrue(userRepository.findByEmail("missing@example.com").isEmpty());
    }
}
