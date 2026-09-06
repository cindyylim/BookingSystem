package com.example.booking.service.impl;

import com.example.booking.dto.AppointmentRequest;
import com.example.booking.dto.UserAppointmentsResponse;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.Appointment;
import com.example.booking.model.TimeSlot;
import com.example.booking.model.User;
import com.example.booking.repository.AppointmentRepository;
import com.example.booking.repository.TimeSlotRepository;
import com.example.booking.service.AppointmentNotificationService;
import com.example.booking.service.AppointmentService;
import com.example.booking.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserService userService;
    private final AppointmentNotificationService notificationService;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
            TimeSlotRepository timeSlotRepository,
            UserService userService,
            AppointmentNotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Override
    public List<Appointment> getAppointmentsVisibleTo(String username) {
        User actor = requireUser(username);
        if (isAdmin(actor)) {
            return appointmentRepository.findAll();
        }
        return appointmentRepository.findByUserId(actor.getId());
    }

    @Override
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    @Override
    public Optional<Appointment> getAppointmentForUser(Long id, String username) {
        User actor = requireUser(username);
        return appointmentRepository.findById(id).filter(appointment -> canManage(appointment, actor));
    }

    @Override
    public UserAppointmentsResponse getUpcomingAndHistoryForUser(Long userId) {
        List<Appointment> all = appointmentRepository.findByUserId(userId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<Appointment> upcoming = all.stream()
                .filter(a -> a.getStartTime() != null && a.getStartTime().isAfter(now))
                .collect(Collectors.toList());
        List<Appointment> history = all.stream()
                .filter(a -> a.getStartTime() != null && !a.getStartTime().isAfter(now))
                .collect(Collectors.toList());
        return new UserAppointmentsResponse(upcoming, history);
    }

    @Override
    @Transactional
    public Appointment bookAppointmentForUser(String username, AppointmentRequest request) {
        User user = userService.getUserByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Appointment appointment = new Appointment();
        appointment.setCustomerName(request.getCustomerName());
        appointment.setCustomerEmail(request.getCustomerEmail());
        appointment.setCustomerPhone(request.getCustomerPhone());
        appointment.setLocation(request.getLocation());
        appointment.setService(request.getService());

        return bookAppointment(appointment, request.getTimeSlotId(), user);
    }

    @Override
    @Transactional
    public Appointment bookAppointment(Appointment appointment, Long timeSlotId, User user) {
        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }
        User actor = requireUser(user.getUsername());

        int updated = timeSlotRepository.markAsUnavailableIfAvailable(timeSlotId);

        if (updated == 0) {
            if (!timeSlotRepository.existsById(timeSlotId)) {
                throw new IllegalArgumentException("Time slot not found");
            }
            throw new ConflictException("Time slot is not available");
        }

        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found"));
        appointment.setTimeSlot(timeSlot);
        appointment.setStartTime(timeSlot.getStartTime());
        appointment.setEndTime(timeSlot.getEndTime());
        appointment.setUser(actor);

        appointment.setCancellationToken(UUID.randomUUID().toString());

        Appointment saved = appointmentRepository.save(appointment);
        scheduleConfirmationEmail(saved);
        return saved;
    }

    @Override
    @Transactional
    public void cancelAppointment(Long id, String username) {
        User actor = requireUser(username);
        appointmentRepository.findById(id)
                .filter(appointment -> canManage(appointment, actor))
                .ifPresent(this::deleteAndReleaseSlot);
    }

    @Override
    @Transactional
    public boolean cancelAppointmentByToken(String token) {
        Appointment appointment = appointmentRepository.findByCancellationToken(token);
        if (appointment == null) {
            return false;
        }
        deleteAndReleaseSlot(appointment);
        return true;
    }

    private void deleteAndReleaseSlot(Appointment appointment) {
        Long slotId = appointment.getTimeSlot() != null ? appointment.getTimeSlot().getId() : null;
        appointmentRepository.delete(appointment);
        appointmentRepository.flush();
        if (slotId != null) {
            timeSlotRepository.markAsAvailable(slotId);
        }
    }

    private User requireUser(String username) {
        return userService.getUserByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private static boolean isAdmin(User user) {
        return "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private static boolean canManage(Appointment appointment, User actor) {
        if (isAdmin(actor)) {
            return true;
        }
        return appointment.getUser() != null && actor.getId() != null
                && actor.getId().equals(appointment.getUser().getId());
    }

    private void scheduleConfirmationEmail(Appointment saved) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationService.sendAppointmentConfirmation(saved);
                }
            });
        } else {
            notificationService.sendAppointmentConfirmation(saved);
        }
    }
}
