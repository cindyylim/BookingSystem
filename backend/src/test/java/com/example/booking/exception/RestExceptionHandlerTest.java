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
