package com.example.booking.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.time.OffsetDateTime;

@Entity
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String cancellationToken;
    private String location;
    private String service;

    @Column(columnDefinition = "timestamp with time zone")
    private OffsetDateTime startTime;

    @Column(columnDefinition = "timestamp with time zone")
    private OffsetDateTime endTime;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonBackReference("user-appointments") // Added unique name
    private User user;

    @ManyToOne
    @JoinColumn(name = "time_slot_id") // This creates the foreign key column in your DB
    @JsonBackReference("timeslot-appointments")
    private TimeSlot timeSlot;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCancellationToken() {
        return cancellationToken;
    }

    public void setCancellationToken(String cancellationToken) {
        this.cancellationToken = cancellationToken;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }
}