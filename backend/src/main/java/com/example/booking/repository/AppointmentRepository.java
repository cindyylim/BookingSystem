package com.example.booking.repository;

import com.example.booking.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Appointment findByCancellationToken(String token);

    List<Appointment> findByUserId(Long userId);

    boolean existsByTimeSlot_Id(Long timeSlotId);
}
