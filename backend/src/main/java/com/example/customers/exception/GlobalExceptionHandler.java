package com.example.customers.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for validation errors and other exceptions.
 *
 * <p>Provides consistent error responses across the application.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  /** Handle validation errors from @Valid annotations. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
      MethodArgumentNotValidException ex, WebRequest request) {

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            (error) -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });

    ValidationErrorResponse response =
        new ValidationErrorResponse("Validation failed", errors, LocalDateTime.now());

    return ResponseEntity.badRequest().body(response);
  }

  /** Handle constraint violation errors. */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, WebRequest request) {

    Map<String, String> errors =
        ex.getConstraintViolations().stream()
            .collect(
                Collectors.toMap(
                    violation -> violation.getPropertyPath().toString(),
                    ConstraintViolation::getMessage));

    ValidationErrorResponse response =
        new ValidationErrorResponse("Constraint violation", errors, LocalDateTime.now());

    return ResponseEntity.badRequest().body(response);
  }

  /** Handle illegal argument exceptions. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex, WebRequest request) {

    ErrorResponse response = new ErrorResponse(ex.getMessage(), LocalDateTime.now());

    return ResponseEntity.badRequest().body(response);
  }

  /** Generic error response DTO. */
  public static class ErrorResponse {
    private String message;
    private LocalDateTime timestamp;

    public ErrorResponse(String message, LocalDateTime timestamp) {
      this.message = message;
      this.timestamp = timestamp;
    }

    public String getMessage() {
      return message;
    }

    public LocalDateTime getTimestamp() {
      return timestamp;
    }
  }

  /** Validation error response DTO. */
  public static class ValidationErrorResponse {
    private String message;
    private Map<String, String> errors;
    private LocalDateTime timestamp;

    public ValidationErrorResponse(
        String message, Map<String, String> errors, LocalDateTime timestamp) {
      this.message = message;
      this.errors = errors;
      this.timestamp = timestamp;
    }

    public String getMessage() {
      return message;
    }

    public Map<String, String> getErrors() {
      return errors;
    }

    public LocalDateTime getTimestamp() {
      return timestamp;
    }
  }
}
