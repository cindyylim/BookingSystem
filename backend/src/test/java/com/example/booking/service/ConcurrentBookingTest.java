package com.example.booking.service;

import com.example.booking.model.Appointment;
import com.example.booking.model.TimeSlot;
import com.example.booking.model.User;
import com.example.booking.repository.AppointmentRepository;
import com.example.booking.repository.TimeSlotRepository;
import com.example.booking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify the atomic booking implementation prevents race
 * conditions.
 */
@SpringBootTest
@ActiveProfiles("test")
public class ConcurrentBookingTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * This test simulates 10 concurrent users trying to book the same time slot.
     * With the atomic update implementation, only ONE should succeed.
     */
    @Test
    public void testConcurrentBooking_OnlyOneSucceeds() throws InterruptedException {
        // Setup: Create a time slot
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setStartTime(OffsetDateTime.now().plusDays(1));
        timeSlot.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(1));
        timeSlot.setAvailable(true);
        TimeSlot savedSlot = timeSlotRepository.save(timeSlot);
        Long timeSlotId = savedSlot.getId();
        User booker = saveUser("concurrent-booker", "concurrent-booker@test.com");

        // Test: Launch 10 concurrent booking attempts
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    Appointment appointment = new Appointment();
                    appointment.setCustomerName("User " + userId);
                    appointment.setCustomerEmail("user" + userId + "@test.com");
                    appointment.setCustomerPhone("555-000" + userId);
                    appointment.setLocation("Test Location");
                    appointment.setService("Test Service");

                    appointmentService.bookAppointment(appointment, timeSlotId, booker);
                    successCount.incrementAndGet();
                    System.out.println("✓ User " + userId + " successfully booked the slot");
                } catch (com.example.booking.exception.ConflictException | IllegalStateException e) {
                    failureCount.incrementAndGet();
                    System.out.println("✗ User " + userId + " failed (slot unavailable)");
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("✗ User " + userId + " failed with error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        latch.await();
        executor.shutdown();

        // Verify: Only ONE booking should succeed
        System.out.println("\n=== Test Results ===");
        System.out.println("Successful bookings: " + successCount.get());
        System.out.println("Failed bookings: " + failureCount.get());

        assertEquals(1, successCount.get(), "Exactly ONE user should successfully book the slot");
        assertEquals(numThreads - 1, failureCount.get(), "All other users should fail");

        // Verify the time slot is now unavailable
        TimeSlot updatedSlot = timeSlotRepository.findById(timeSlotId).orElseThrow();
        assertFalse(updatedSlot.isAvailable(), "Time slot should be marked as unavailable");

        // Verify only one appointment was created for this slot
        long appointmentCount = appointmentRepository.findAll().stream()
                .filter(a -> a.getTimeSlot() != null && a.getTimeSlot().getId().equals(timeSlotId))
                .count();
        assertEquals(1, appointmentCount, "Exactly ONE appointment should exist for this time slot");
    }

    /**
     * Test that booking a non-existent time slot throws the correct exception.
     */
    @Test
    public void testBookNonExistentSlot_ThrowsException() {
        Appointment appointment = new Appointment();
        appointment.setCustomerName("Test User");
        appointment.setCustomerEmail("test@test.com");
        appointment.setCustomerPhone("555-0000");

        assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.bookAppointment(appointment, 99999L, saveUser("missing-slot-user", "missing-slot@test.com"));
        }, "Booking non-existent slot should throw IllegalArgumentException");
    }

    /**
     * Test that booking an already unavailable slot throws the correct exception.
     */
    @Test
    @Transactional
    public void testBookUnavailableSlot_ThrowsException() {
        // Create an unavailable time slot
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setStartTime(OffsetDateTime.now().plusDays(1));
        timeSlot.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(1));
        timeSlot.setAvailable(false);
        TimeSlot savedSlot = timeSlotRepository.save(timeSlot);

        Appointment appointment = new Appointment();
        appointment.setCustomerName("Test User");
        appointment.setCustomerEmail("test@test.com");
        appointment.setCustomerPhone("555-0000");

        assertThrows(com.example.booking.exception.ConflictException.class, () -> {
            appointmentService.bookAppointment(appointment, savedSlot.getId(),
                    saveUser("unavailable-slot-user", "unavailable-slot@test.com"));
        }, "Booking unavailable slot should throw ConflictException");
    }

    private User saveUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("secret");
        user.setEmail(email);
        user.setPhone("555-0000");
        user.setRole("USER");
        return userRepository.save(user);
    }
}
