package com.example.booking.service;

import com.example.booking.model.Appointment;
import com.example.booking.model.TimeSlot;
import com.example.booking.model.User;
import com.example.booking.repository.AppointmentRepository;
import com.example.booking.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppointmentService.
 * Tests the booking logic, validation, and atomic update operations.
 */
@ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AppointmentService appointmentService;

    private TimeSlot availableTimeSlot;
    private TimeSlot unavailableTimeSlot;
    private Appointment testAppointment;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Setup available time slot
        availableTimeSlot = new TimeSlot();
        availableTimeSlot.setId(1L);
        availableTimeSlot.setStartTime(OffsetDateTime.now().plusDays(1));
        availableTimeSlot.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(1));
        availableTimeSlot.setAvailable(true);

        // Setup unavailable time slot
        unavailableTimeSlot = new TimeSlot();
        unavailableTimeSlot.setId(2L);
        unavailableTimeSlot.setStartTime(OffsetDateTime.now().plusDays(2));
        unavailableTimeSlot.setEndTime(OffsetDateTime.now().plusDays(2).plusHours(1));
        unavailableTimeSlot.setAvailable(false);

        // Setup test appointment
        testAppointment = new Appointment();
        testAppointment.setCustomerName("John Doe");
        testAppointment.setCustomerEmail("john@example.com");
        testAppointment.setCustomerPhone("555-1234");
        testAppointment.setLocation("Office A");
        testAppointment.setService("Consultation");

        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        // Mock MimeMessage creation to prevent NPE
        lenient().when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
    }

    @Test
    void testBookAppointment_Success_WithUser() {
        // Arrange
        when(timeSlotRepository.markAsUnavailableIfAvailable(1L)).thenReturn(1);
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(availableTimeSlot));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        // Act
        Appointment result = appointmentService.bookAppointment(testAppointment, 1L, testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(availableTimeSlot, result.getTimeSlot());
        assertNotNull(result.getCancellationToken());
        assertEquals("John Doe", result.getCustomerName());

        // Verify interactions
        verify(timeSlotRepository).markAsUnavailableIfAvailable(1L);
        verify(timeSlotRepository).findById(1L);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testBookAppointment_Success_WithoutUser() {
        // Arrange
        when(timeSlotRepository.markAsUnavailableIfAvailable(1L)).thenReturn(1);
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(availableTimeSlot));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        // Act
        Appointment result = appointmentService.bookAppointment(testAppointment, 1L, null);

        // Assert
        assertNotNull(result);
        assertNull(result.getUser());
        assertEquals(availableTimeSlot, result.getTimeSlot());
        assertNotNull(result.getCancellationToken());

        // Verify interactions
        verify(timeSlotRepository).markAsUnavailableIfAvailable(1L);
        verify(timeSlotRepository).findById(1L);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testBookAppointment_FailsWhenSlotAlreadyBooked() {
        // Arrange - atomic update returns 0 (slot already unavailable)
        when(timeSlotRepository.markAsUnavailableIfAvailable(2L)).thenReturn(0);
        when(timeSlotRepository.existsById(2L)).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            appointmentService.bookAppointment(testAppointment, 2L, testUser);
        });

        assertEquals("Time slot is not available", exception.getMessage());

        // Verify
        verify(timeSlotRepository).markAsUnavailableIfAvailable(2L);
        verify(timeSlotRepository).existsById(2L);
        verify(appointmentRepository, never()).save(any());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testBookAppointment_FailsWhenSlotNotFound() {
        // Arrange - atomic update returns 0 and slot doesn't exist
        when(timeSlotRepository.markAsUnavailableIfAvailable(999L)).thenReturn(0);
        when(timeSlotRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.bookAppointment(testAppointment, 999L, testUser);
        });

        assertEquals("Time slot not found", exception.getMessage());

        // Verify
        verify(timeSlotRepository).markAsUnavailableIfAvailable(999L);
        verify(timeSlotRepository).existsById(999L);
        verify(appointmentRepository, never()).save(any());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testBookAppointment_GeneratesCancellationToken() {
        // Arrange
        when(timeSlotRepository.markAsUnavailableIfAvailable(1L)).thenReturn(1);
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(availableTimeSlot));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        // Act
        Appointment result = appointmentService.bookAppointment(testAppointment, 1L, testUser);

        // Assert
        assertNotNull(result.getCancellationToken());
        assertTrue(result.getCancellationToken().length() > 0);
    }

    @Test
    void testCancelAppointment_Success() {
        // Arrange
        Appointment appointment = new Appointment();
        appointment.setId(100L);
        appointment.setTimeSlot(unavailableTimeSlot);

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));
        doNothing().when(appointmentRepository).delete(appointment);

        // Act
        appointmentService.cancelAppointment(100L);

        // Assert
        verify(appointmentRepository).findById(100L);
        verify(timeSlotRepository).save(unavailableTimeSlot);
        verify(appointmentRepository).delete(appointment);
        assertTrue(unavailableTimeSlot.isAvailable(), "Time slot should be marked as available after cancellation");
    }

    @Test
    void testCancelAppointment_WhenNotFound() {
        // Arrange
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        appointmentService.cancelAppointment(999L);

        // Assert
        verify(appointmentRepository).findById(999L);
        verify(appointmentRepository, never()).delete(any());
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    void testCancelAppointmentByToken_Success() {
        // Arrange
        String token = "test-token-123";
        Appointment appointment = new Appointment();
        appointment.setId(100L);
        appointment.setCancellationToken(token);
        appointment.setTimeSlot(unavailableTimeSlot);

        when(appointmentRepository.findByCancellationToken(token)).thenReturn(appointment);
        doNothing().when(appointmentRepository).delete(appointment);

        // Act
        boolean result = appointmentService.cancelAppointmentByToken(token);

        // Assert
        assertTrue(result);
        verify(appointmentRepository).findByCancellationToken(token);
        verify(timeSlotRepository).save(unavailableTimeSlot);
        verify(appointmentRepository).delete(appointment);
        assertTrue(unavailableTimeSlot.isAvailable());
    }

    @Test
    void testCancelAppointmentByToken_SuccessNullTimeSlot() {
        // Arrange
        String token = "test-token-123";
        Appointment appointment = new Appointment();
        appointment.setId(100L);
        appointment.setCancellationToken(token);
        appointment.setTimeSlot(null);

        when(appointmentRepository.findByCancellationToken(token)).thenReturn(appointment);
        doNothing().when(appointmentRepository).delete(appointment);

        // Act
        boolean result = appointmentService.cancelAppointmentByToken(token);

        // Assert
        assertTrue(result);
        verify(appointmentRepository).findByCancellationToken(token);
        verify(timeSlotRepository, never()).save(any());
        verify(appointmentRepository).delete(appointment);
    }

    @Test
    void testCancelAppointmentByToken_InvalidToken() {
        // Arrange
        when(appointmentRepository.findByCancellationToken("invalid-token")).thenReturn(null);

        // Act
        boolean result = appointmentService.cancelAppointmentByToken("invalid-token");

        // Assert
        assertFalse(result);
        verify(appointmentRepository).findByCancellationToken("invalid-token");
        verify(appointmentRepository, never()).delete(any());
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    void testCancelAppointment_WithNullTimeSlot() {
        // Arrange
        Appointment appointment = new Appointment();
        appointment.setId(100L);
        appointment.setTimeSlot(null);

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));

        // Act
        appointmentService.cancelAppointment(100L);

        // Assert
        verify(appointmentRepository).findById(100L);
        verify(timeSlotRepository, never()).save(any());
        verify(appointmentRepository).delete(appointment);
    }

    @Test
    void testGetAllAppointments() {
        // Arrange
        when(appointmentRepository.findAll()).thenReturn(java.util.List.of(testAppointment));

        // Act
        var result = appointmentService.getAllAppointments();

        // Assert
        assertEquals(1, result.size());
        verify(appointmentRepository).findAll();
    }

    @Test
    void testGetAppointment() {
        // Arrange
        testAppointment.setId(100L);
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(testAppointment));

        // Act
        Optional<Appointment> result = appointmentService.getAppointment(100L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(100L, result.get().getId());
        verify(appointmentRepository).findById(100L);
    }

    @Test
    void testGetAppointmentsForUser() {
        // Arrange
        Long userId = 1L;
        when(appointmentRepository.findByUserId(userId)).thenReturn(java.util.List.of(testAppointment));

        // Act
        var result = appointmentService.getAppointmentsForUser(userId);

        // Assert
        assertEquals(1, result.size());
        verify(appointmentRepository).findByUserId(userId);
    }
}
