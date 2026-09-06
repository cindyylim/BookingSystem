package com.example.booking.service;

import com.example.booking.model.Appointment;
import com.example.booking.model.TimeSlot;
import com.example.booking.model.User;
import com.example.booking.repository.TimeSlotRepository;
import com.example.booking.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AppointmentBookingEmailFailureTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void bookAppointmentSucceedsWhenMailSenderThrows() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(43).withNano(0);
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(start);
        slot.setEndTime(start.plusHours(1));
        slot.setAvailable(true);
        slot = timeSlotRepository.save(slot);

        Appointment appointment = new Appointment();
        appointment.setCustomerName("Mail Fail User");
        appointment.setCustomerEmail("mailfail@test.com");
        appointment.setCustomerPhone("555-0300");
        appointment.setLocation("Office");
        appointment.setService("Consult");

        Appointment saved = appointmentService.bookAppointment(appointment, slot.getId(),
                saveUser("mailfail-user", "mailfail@test.com"));

        assertNotNull(saved.getId());
        assertFalse(timeSlotRepository.findById(slot.getId()).orElseThrow().isAvailable());
    }

    private User saveUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("secret");
        user.setEmail(email);
        user.setPhone("555-0300");
        user.setRole("USER");
        return userRepository.save(user);
    }
}
