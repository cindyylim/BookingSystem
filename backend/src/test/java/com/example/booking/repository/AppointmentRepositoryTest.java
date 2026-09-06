package com.example.booking.repository;

import com.example.booking.model.Appointment;
import com.example.booking.model.TimeSlot;
import com.example.booking.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void findByCancellationToken() {
        Appointment saved = appointmentRepository.save(baseAppointment("tok-abc", null, null));

        Appointment found = appointmentRepository.findByCancellationToken("tok-abc");
        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertNull(appointmentRepository.findByCancellationToken("missing"));
    }

    @Test
    void findByUserId() {
        User user = saveUser("repo-user", "repo-user@example.com");
        appointmentRepository.save(baseAppointment("t1", user, null));
        appointmentRepository.save(baseAppointment("t2", user, null));
        appointmentRepository.save(baseAppointment("t3", saveUser("other", "other@example.com"), null));

        List<Appointment> forUser = appointmentRepository.findByUserId(user.getId());
        assertEquals(2, forUser.size());
    }

    @Test
    void existsByTimeSlotId() {
        User user = saveUser("repo-user", "repo-user@example.com");

        TimeSlot slot = saveSlot(OffsetDateTime.now().plusDays(3));
        assertFalse(appointmentRepository.existsByTimeSlot_Id(slot.getId()));

        appointmentRepository.save(baseAppointment("slot-tok", user, slot));
        assertTrue(appointmentRepository.existsByTimeSlot_Id(slot.getId()));
    }

    private User saveUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("secret");
        user.setEmail(email);
        user.setPhone("1234567890");
        user.setRole("USER");
        return userRepository.save(user);
    }

    private TimeSlot saveSlot(OffsetDateTime start) {
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(start);
        slot.setEndTime(start.plusHours(1));
        slot.setAvailable(true);
        return timeSlotRepository.save(slot);
    }

    private Appointment baseAppointment(String token, User user, TimeSlot slot) {
        Appointment appointment = new Appointment();
        appointment.setCustomerName("John Doe");
        appointment.setCustomerEmail("john@example.com");
        appointment.setCustomerPhone("555-1234");
        appointment.setCancellationToken(token);
        appointment.setService("Cut");
        appointment.setLocation("Main");
        appointment.setUser(user);
        appointment.setTimeSlot(slot);
        if (slot != null) {
            appointment.setStartTime(slot.getStartTime());
            appointment.setEndTime(slot.getEndTime());
        }
        return appointment;
    }
}
