package com.example.booking.controller;

import com.example.booking.model.TimeSlot;
import com.example.booking.service.TimeSlotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.time.Instant;
import java.util.List;
import com.example.booking.mapper.TimeSlotMapper;
import com.example.booking.dto.TimeSlotDTO;
import com.example.booking.dto.TimeSlotDTO;
import com.example.booking.mapper.TimeSlotMapper;
import com.example.booking.model.TimeSlot;
import com.example.booking.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timeslots")
public class TimeSlotController {
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    private final TimeSlotService timeSlotService;

    public TimeSlotController(TimeSlotService timeSlotService) {
        this.timeSlotService = timeSlotService;
    }

    @GetMapping
    public List<TimeSlotDTO> getAllTimeSlots() {
        List<TimeSlotDTO> dtos = timeSlotRepository.findAll().stream()
                .map(TimeSlotMapper::toDTO)
                .collect(Collectors.toList());
        return dtos;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimeSlotDTO> getTimeSlot(@PathVariable Long id) {
        TimeSlot timeSlot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TimeSlot not found"));

        TimeSlotDTO dto = TimeSlotMapper.toDTO(timeSlot);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<?> createTimeSlot(@RequestBody TimeSlotRequest request) {
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setStartTime(OffsetDateTime.parse(request.getStartTime()));
        timeSlot.setEndTime(OffsetDateTime.parse(request.getEndTime()));
        timeSlot.setAvailable(request.isAvailable());

        timeSlotService.validateTimeSlot(timeSlot);

        if (timeSlotService.isOverlapping(timeSlot)) {
            return ResponseEntity.badRequest().body("Time slot overlaps with an existing slot.");
        }

        return ResponseEntity.ok(timeSlotService.createTimeSlot(timeSlot));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateTimeSlot(@PathVariable Long id, @RequestBody TimeSlot timeSlot) {
        timeSlotService.validateTimeSlot(timeSlot);

        if (timeSlotService.isOverlapping(timeSlot, id)) {
            return ResponseEntity.badRequest().body("Updated time slot overlaps with an existing slot.");
        }

        try {
            return ResponseEntity.ok(timeSlotService.updateTimeSlot(id, timeSlot));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTimeSlot(@PathVariable Long id) {
        if (timeSlotService.isBooked(id)) {
            return ResponseEntity.badRequest().body("Cannot delete a time slot that is already booked.");
        }

        timeSlotService.deleteTimeSlot(id);
        return ResponseEntity.noContent().build();
    }

    public static class TimeSlotRequest {
        private String startTime;
        private String endTime;
        private boolean available;

        public TimeSlotRequest() {
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }
    }

}
