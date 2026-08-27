package com.example.booking.service;

import com.example.booking.model.Appointment;
import com.example.booking.service.impl.EmailAppointmentNotificationService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.OffsetDateTime;
import java.util.Properties;

import org.springframework.mail.MailSendException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailAppointmentNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailAppointmentNotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new EmailAppointmentNotificationService(mailSender, "https://booking.example");
    }

    @Test
    void sendAppointmentConfirmation_NullTo_DoesNotSend() {
        Appointment appointment = new Appointment();
        appointment.setCustomerEmail(null);

        notificationService.sendAppointmentConfirmation(appointment);

        verifyNoInteractions(mailSender);
    }

    @Test
    void sendAppointmentConfirmation_BlankTo_DoesNotSend() {
        Appointment appointment = new Appointment();
        appointment.setCustomerEmail("   ");

        notificationService.sendAppointmentConfirmation(appointment);

        verifyNoInteractions(mailSender);
    }

    @Test
    void sendAppointmentConfirmation_SendsMail() throws Exception {
        Appointment appointment = new Appointment();
        appointment.setCustomerName("Jane");
        appointment.setCustomerEmail("jane@example.com");
        appointment.setCancellationToken("tok-1");
        appointment.setStartTime(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        appointment.setEndTime(OffsetDateTime.parse("2024-01-01T11:00:00Z"));

        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        notificationService.sendAppointmentConfirmation(appointment);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(message);
    }

    @Test
    void sendAppointmentConfirmation_SendThrows_SwallowsError() {
        Appointment appointment = new Appointment();
        appointment.setCustomerName("Jane");
        appointment.setCustomerEmail("jane@example.com");
        appointment.setCancellationToken("tok-1");
        appointment.setStartTime(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        appointment.setEndTime(OffsetDateTime.parse("2024-01-01T11:00:00Z"));

        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(message);

        assertDoesNotThrow(() -> notificationService.sendAppointmentConfirmation(appointment));
        verify(mailSender).send(message);
    }
}
