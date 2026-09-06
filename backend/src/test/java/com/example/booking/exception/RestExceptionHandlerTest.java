package com.example.booking.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.format.DateTimeParseException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void resourceNotFoundReturns404() {
        ResponseEntity<Map<String, String>> response = handler.handleNotFound(
                new ResourceNotFoundException("TimeSlot not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("TimeSlot not found", response.getBody().get("message"));
        assertEquals("TimeSlot not found", response.getBody().get("error"));
    }

    @Test
    void conflictReturns409() {
        ResponseEntity<Object> response = handler.handleConflict(
                new ConflictException("Time slot is not available"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Time slot is not available", body.get("message"));
        assertEquals("Time slot is not available", body.get("error"));
    }

    @Test
    void unauthorizedReturns401() {
        ResponseEntity<Map<String, String>> response = handler.handleUnauthorized(
                new UnauthorizedException("Invalid credentials"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody().get("message"));
        assertEquals("Invalid credentials", response.getBody().get("error"));
    }

    @Test
    void illegalArgumentExceptionReturns400() {
        ResponseEntity<Map<String, String>> response = handler.handleBadRequest(
                new IllegalArgumentException("Time slot overlaps with an existing slot."));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Time slot overlaps with an existing slot.", response.getBody().get("message"));
        assertEquals("Time slot overlaps with an existing slot.", response.getBody().get("error"));
    }

    @Test
    void illegalArgumentExceptionWithoutMessageUsesFallback() {
        ResponseEntity<Map<String, String>> response = handler.handleBadRequest(
                new IllegalArgumentException((String) null));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad request", response.getBody().get("message"));
        assertEquals("Bad request", response.getBody().get("error"));
    }

    @Test
    void dateTimeParseExceptionReturns400() {
        ResponseEntity<Map<String, String>> response = handler.handleBadRequest(
                new DateTimeParseException("bad", "x", 0));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid date/time format", response.getBody().get("message"));
    }

    @Test
    void unexpectedExceptionDoesNotLeakMessage() {
        ResponseEntity<Map<String, String>> response = handler.handleUnexpected(
                new RuntimeException("sql dump"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal server error", response.getBody().get("message"));
    }

    @Test
    void handleValidation_NoFieldErrors_UsesFallbackMessage() throws Exception {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        MethodParameter parameter = new MethodParameter(
                RestExceptionHandler.class.getDeclaredMethod("handleValidation", MethodArgumentNotValidException.class),
                0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().get("message"));
        assertEquals("Validation failed", response.getBody().get("error"));
    }

    @Test
    void handleValidation_FieldError_UsesFieldMessage() throws Exception {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));
        MethodParameter parameter = new MethodParameter(
                RestExceptionHandler.class.getDeclaredMethod("handleValidation", MethodArgumentNotValidException.class),
                0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("email must not be blank", response.getBody().get("message"));
    }
}
