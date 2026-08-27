package com.example.booking.service.impl;

import com.example.booking.model.Appointment;
import com.example.booking.service.AppointmentNotificationService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailAppointmentNotificationService implements AppointmentNotificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailAppointmentNotificationService.class);

    private final JavaMailSender mailSender;
    private final String baseUrl;

    public EmailAppointmentNotificationService(JavaMailSender mailSender,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.mailSender = mailSender;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public void sendAppointmentConfirmation(Appointment appointment) {
        String to = appointment.getCustomerEmail();
        if (to == null || to.isBlank()) {
            return;
        }
        String subject = "Appointment Confirmation & Cancellation Link";
        String cancelUrl = baseUrl + "/api/appointments/cancel/" + appointment.getCancellationToken();
        String text = String.format(
                "Dear %s,%n%nYour appointment is confirmed for %s - %s.%n%nIf you wish to cancel, click here: %s%n%nThank you!",
                appointment.getCustomerName(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                cancelUrl);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Email failed: {}", e.getMessage());
        }
    }
}
