package com.example.booking.service;

import com.example.booking.dto.AppointmentRequest;
import com.example.booking.exception.ConflictException;
import com.example.booking.model.Appointment;
import com.example.booking.model.TimeSlot;
import com.example.booking.model.User;
import com.example.booking.repository.AppointmentRepository;
import com.example.booking.repository.TimeSlotRepository;
import com.example.booking.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private UserService userService;

    @Mock
    private AppointmentNotificationService notificationService;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private TimeSlot availableTimeSlot;
    private TimeSlot unavailableTimeSlot;
    private Appointment testAppointment;
    private User testUser;

    @BeforeEach
    void setUp() {
        availableTimeSlot = new TimeSlot();
        availableTimeSlot.setId(1L);
        availableTimeSlot.setStartTime(OffsetDateTime.now().plusDays(1));
        availableTimeSlot.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(1));
        availableTimeSlot.setAvailable(true);

        unavailableTimeSlot = new TimeSlot();
        unavailableTimeSlot.setId(2L);
        unavailableTimeSlot.setStartTime(OffsetDateTime.now().plusDays(2));
        unavailableTimeSlot.setEndTime(OffsetDateTime.now().plusDays(2).plusHours(1));
        unavailableTimeSlot.setAvailable(false);

        testAppointment = new Appointment();
        testAppointment.setCustomerName("John Doe");
        testAppointment.setCustomerEmail("john@example.com");
        testAppointment.setCustomerPhone("555-1234");
        testAppointment.setLocation("Office A");
        testAppointment.setService("Consultation");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("john@example.com");
        testUser.setPhone("555-1234");
    }

    @Test
    void testBookAppointment_Success_WithUser() {
        when(timeSlotRepository.markAsUnavailableIfAvailable(1L)).thenReturn(1);
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(availableTimeSlot));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        Appointment result = appointmentService.bookAppointment(testAppointment, 1L, testUser);

        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(availableTimeSlot, result.getTimeSlot());
        assertNotNull(result.getCancellationToken());
        assertEquals("John Doe", result.getCustomerName());

        verify(timeSlotRepository).markAsUnavailableIfAvailable(1L);
        verify(timeSlotRepository).findById(1L);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(notificationService).sendAppointmentConfirmation(any(Appointment.class));
    }

    @Test
    void testBookAppointment_Success_WithoutUser() {
        when(timeSlotRepository.markAsUnavailableIfAvailable(1L)).thenReturn(1);
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(availableTimeSlot));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        Appointment result = appointmentService.bookAppointment(testAppointment, 1L, null);

        assertNotNull(result);
        assertNull(result.getUser());
        assertEquals(availableTimeSlot, result.getTimeSlot());
        assertNotNull(result.getCancellationToken());

        verify(notificationService).sendAppointmentConfirmation(any(Appointment.class));
    }

    @Test
    void testBookAppointmentForUser() {
        AppointmentRequest request = new AppointmentRequest();
        request.setTimeSlotId(1L);
        request.setLocation("Office A");
        request.setService("Consultation");

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(timeSlotRepository.markAsUnavailableIfAvailable(1L)).thenReturn(1);
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(availableTimeSlot));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = appointmentService.bookAppointmentForUser("testuser", request);

        assertEquals(testUser, result.getUser());
        assertEquals("testuser", result.getCustomerName());
    }

    @Test
    void testBookAppointment_FailsWhenSlotAlreadyBooked() {
        when(timeSlotRepository.markAsUnavailableIfAvailable(2L)).thenReturn(0);
        when(timeSlotRepository.existsById(2L)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            appointmentService.bookAppointment(testAppointment, 2L, testUser);
        });

        assertEquals("Time slot is not available", exception.getMessage());
        verify(appointmentRepository, never()).save(any());
        verify(notificationService, never()).sendAppointmentConfirmation(any());
    }

    @Test
    void testBookAppointment_FailsWhenSlotNotFound() {
        when(timeSlotRepository.markAsUnavailableIfAvailable(999L)).thenReturn(0);
        when(timeSlotRepository.existsById(999L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.bookAppointment(testAppointment, 999L, testUser);
        });

        assertEquals("Time slot not found", exception.getMessage());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void testBookAppointment_GeneratesCancellationToken() {
        when(timeSlotRepository.markAsUnavailableIfAvailable(1L)).thenReturn(1);
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(availableTimeSlot));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = appointmentService.bookAppointment(testAppointment, 1L, testUser);

        assertNotNull(result.getCancellationToken());
        assertTrue(result.getCancellationToken().length() > 0);
    }

    @Test
    void testCancelAppointment_Success() {
        Appointment appointment = new Appointment();
        appointment.setId(100L);
        appointment.setTimeSlot(unavailableTimeSlot);

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.existsByTimeSlot_Id(2L)).thenReturn(false);

        appointmentService.cancelAppointment(100L);

        verify(appointmentRepository).delete(appointment);
        verify(timeSlotRepository).markAsAvailable(2L);
    }

    @Test
    void testCancelAppointment_WhenNotFound() {
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        appointmentService.cancelAppointment(999L);

        verify(appointmentRepository).findById(999L);
        verify(appointmentRepository, never()).delete(any());
        verify(timeSlotRepository, never()).markAsAvailable(any());
    }

    @Test
    void testCancelAppointmentByToken_Success() {
        String token = "test-token-123";
        Appointment appointment = new Appointment();
        appointment.setId(100L);
        appointment.setCancellationToken(token);
        appointment.setTimeSlot(unavailableTimeSlot);

        when(appointmentRepository.findByCancellationToken(token)).thenReturn(appointment);
        when(appointmentRepository.existsByTimeSlot_Id(2L)).thenReturn(false);

        boolean result = appointmentService.cancelAppointmentByToken(token);

        assertTrue(result);
        verify(appointmentRepository).delete(appointment);
        verify(timeSlotRepository).markAsAvailable(2L);
    }

    @Test
    void testCancelAppointmentByToken_SuccessNullTimeSlot() {
        String token = "test-token-123";
        Appointment appointment = new Appointment();
        appointment.setId(100L);
        appointment.setCancellationToken(token);
        appointment.setTimeSlot(null);

        when(appointmentRepository.findByCancellationToken(token)).thenReturn(appointment);

        boolean result = appointmentService.cancelAppointmentByToken(token);

        assertTrue(result);
        verify(timeSlotRepository, never()).markAsAvailable(any());
        verify(appointmentRepository).delete(appointment);
    }

    @Test
    void testCancelAppointmentByToken_InvalidToken() {
        when(appointmentRepository.findByCancellationToken("invalid-token")).thenReturn(null);

        boolean result = appointmentService.cancelAppointmentByToken("invalid-token");

        assertFalse(result);
        verify(appointmentRepository, never()).delete(any());
    }

    @Test
    void testCancelAppointment_WithNullTimeSlot() {
        Appointment appointment = new Appointment();
        appointment.setId(100L);
        appointment.setTimeSlot(null);

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));

        appointmentService.cancelAppointment(100L);

        verify(appointmentRepository).delete(appointment);
        verify(timeSlotRepository, never()).markAsAvailable(any());
    }

    @Test
    void testGetAllAppointments() {
        when(appointmentRepository.findAll()).thenReturn(java.util.List.of(testAppointment));

        var result = appointmentService.getAllAppointments();

        assertEquals(1, result.size());
        verify(appointmentRepository).findAll();
    }

    @Test
    void testGetAppointment() {
        testAppointment.setId(100L);
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(testAppointment));

        Optional<Appointment> result = appointmentService.getAppointment(100L);

        assertTrue(result.isPresent());
        assertEquals(100L, result.get().getId());
    }

    @Test
    void testGetAppointmentsForUser() {
        Long userId = 1L;
        when(appointmentRepository.findByUserId(userId)).thenReturn(java.util.List.of(testAppointment));

        var result = appointmentService.getAppointmentsForUser(userId);

        assertEquals(1, result.size());
        verify(appointmentRepository).findByUserId(userId);
    }
}
