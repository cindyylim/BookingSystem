package com.example.booking.service;

import com.example.booking.model.Appointment;
import com.example.booking.model.TimeSlot;
import com.example.booking.repository.AppointmentRepository;
import com.example.booking.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

import com.example.booking.model.User;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final JavaMailSender mailSender;

    public AppointmentService(AppointmentRepository appointmentRepository, TimeSlotRepository timeSlotRepository,
            JavaMailSender mailSender) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.mailSender = mailSender;
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public List<Appointment> getAppointmentsForUser(Long userId) {
        return appointmentRepository.findByUserId(userId);
    }

    public Optional<Appointment> getAppointment(Long id) {
        return appointmentRepository.findById(id);
    }

    @Transactional
    public Appointment bookAppointment(Appointment appointment, Long timeSlotId, User user) {
        System.out.println("Booking appointment for slot ID: " + timeSlotId
                + (user != null ? " for user: " + user.getUsername() : " as Guest"));

        // Atomic check-and-set: mark as unavailable only if currently available
        int updated = timeSlotRepository.markAsUnavailableIfAvailable(timeSlotId);

        if (updated == 0) {
            if (!timeSlotRepository.existsById(timeSlotId)) {
                throw new IllegalArgumentException("Time slot not found");
            }
            throw new IllegalStateException("Time slot is not available");
        }

        // Slot is now reserved for this booking, complete the appointment
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId).orElseThrow();
        appointment.setTimeSlot(timeSlot); // Link the association (Owning side)

        // It's good practice to set both sides of the relationship
        if (timeSlot.getAppointments() != null) {
            timeSlot.getAppointments().add(appointment);
        }

        appointment.setStartTime(timeSlot.getStartTime());
        appointment.setEndTime(timeSlot.getEndTime());

        if (user != null) {
            appointment.setUser(user);
        }

        // Generate cancellation token
        String token = UUID.randomUUID().toString();
        appointment.setCancellationToken(token);

        Appointment saved = appointmentRepository.save(appointment);
        System.out.println("Saved appointment ID: " + saved.getId() + " with TimeSlot ID: "
                + (saved.getTimeSlot() != null ? saved.getTimeSlot().getId() : "NULL"));

        // Send email
        sendAppointmentEmail(saved);
        return saved;
    }

    private void sendAppointmentEmail(Appointment appointment) {
        String to = appointment.getCustomerEmail();
        String subject = "Appointment Confirmation & Cancellation Link";
        String cancelUrl = "http://localhost:8080/api/appointments/cancel/" + appointment.getCancellationToken();
        String text = String.format(
                "Dear %s,\n\nYour appointment is confirmed for %s - %s.\n\nIf you wish to cancel, click here: %s\n\nThank you!",
                appointment.getCustomerName(),
                appointment.getStartTime().toString(),
                appointment.getEndTime().toString(),
                cancelUrl);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Email failed: " + e.getMessage());
        }
    }

    @Transactional
    public void cancelAppointment(Long id) {
        appointmentRepository.findById(id).ifPresent(appointment -> {
            TimeSlot slot = appointment.getTimeSlot();
            if (slot == null) {
                // Fallback for appointments where the FK was null
                slot = timeSlotRepository.findByTime(appointment.getStartTime(), appointment.getEndTime());
            }

            if (slot != null) {
                slot.setAvailable(true);
                timeSlotRepository.save(slot);
            }
            appointmentRepository.delete(appointment);
        });
    }

    @Transactional
    public boolean cancelAppointmentByToken(String token) {
        Appointment appointment = appointmentRepository.findByCancellationToken(token);
        if (appointment == null)
            return false;

        TimeSlot slot = appointment.getTimeSlot();
        if (slot == null) {
            slot = timeSlotRepository.findByTime(appointment.getStartTime(), appointment.getEndTime());
        }

        if (slot != null) {
            slot.setAvailable(true);
            timeSlotRepository.save(slot);
        }
        appointmentRepository.delete(appointment);
        return true;
    }
}