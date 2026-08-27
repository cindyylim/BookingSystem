package com.example.booking.mapper;

import com.example.booking.model.TimeSlot;
import com.example.booking.model.Appointment;
import com.example.booking.dto.TimeSlotDTO;
import com.example.booking.dto.AppointmentDTO;

import java.util.List;
import java.util.stream.Collectors;

public class TimeSlotMapper {

    public static TimeSlotDTO toDTO(TimeSlot timeSlot) {
        TimeSlotDTO dto = new TimeSlotDTO();
        dto.setId(timeSlot.getId());
        dto.setStartTime(timeSlot.getStartTime());
        dto.setEndTime(timeSlot.getEndTime());
        dto.setAvailable(timeSlot.isAvailable());

        List<Appointment> source = timeSlot.getAppointments();
        List<AppointmentDTO> appointments = source == null
            ? List.of()
            : source.stream()
                .map(TimeSlotMapper::mapAppointment)
                .collect(Collectors.toList());

        dto.setAppointments(appointments);
        return dto;
    }

    private static AppointmentDTO mapAppointment(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(appointment.getId());
        dto.setClientName(appointment.getCustomerName());
        dto.setClientPhone(appointment.getCustomerPhone());
        dto.setClientEmail(appointment.getCustomerEmail());
        dto.setLocation(appointment.getLocation());
        dto.setService(appointment.getService());

        return dto;
    }
}
