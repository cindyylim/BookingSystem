package com.example.booking.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
public class TimeSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "timestamp with time zone")
    private OffsetDateTime startTime;
    @Column(columnDefinition = "timestamp with time zone")
    private OffsetDateTime endTime;
    private boolean available = true;

    @OneToMany(mappedBy = "timeSlot")
    @JsonManagedReference("timeslot-appointments")
    private List<Appointment> appointments;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public List<Appointment> getAppointments() {
        return this.appointments;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }
}