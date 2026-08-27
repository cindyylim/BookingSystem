package com.example.booking.controller;

import com.example.booking.dto.TimeSlotDTO;
import com.example.booking.dto.TimeSlotRequest;
import com.example.booking.mapper.TimeSlotMapper;
import com.example.booking.model.TimeSlot;
import com.example.booking.service.TimeSlotService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timeslots")
public class TimeSlotController {
    private final TimeSlotService timeSlotService;

    public TimeSlotController(TimeSlotService timeSlotService) {
        this.timeSlotService = timeSlotService;
    }

    @GetMapping
    public List<TimeSlotDTO> getAllTimeSlots(Authentication authentication) {
        boolean admin = isAdmin(authentication);
        List<TimeSlot> slots = admin ? timeSlotService.getAllTimeSlots() : timeSlotService.getAvailableTimeSlots();
        return slots.stream()
                .map(slot -> TimeSlotMapper.toDTO(slot, admin))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimeSlotDTO> getTimeSlot(@PathVariable Long id, Authentication authentication) {
        boolean admin = isAdmin(authentication);
        return timeSlotService.getTimeSlot(id)
                .map(slot -> TimeSlotMapper.toDTO(slot, admin))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TimeSlot> createTimeSlot(@RequestBody TimeSlotRequest request) {
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setStartTime(OffsetDateTime.parse(request.getStartTime()));
        timeSlot.setEndTime(OffsetDateTime.parse(request.getEndTime()));
        timeSlot.setAvailable(request.isAvailable());
        return ResponseEntity.ok(timeSlotService.createTimeSlot(timeSlot));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TimeSlot> updateTimeSlot(@PathVariable Long id, @RequestBody TimeSlot timeSlot) {
        return ResponseEntity.ok(timeSlotService.updateTimeSlot(id, timeSlot));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimeSlot(@PathVariable Long id) {
        timeSlotService.deleteTimeSlot(id);
        return ResponseEntity.noContent().build();
    }

    private static boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
