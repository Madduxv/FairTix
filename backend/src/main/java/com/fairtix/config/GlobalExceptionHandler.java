package com.fairtix.config;

import com.fairtix.inventory.application.SeatHoldConflictException;
import com.fairtix.inventory.application.SeatHoldNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Translates domain and validation exceptions into a consistent JSON error body:
 * <pre>
 * {
 *   "status":    409,
 *   "code":      "HOLD_CONFLICT",
 *   "message":   "Seat ... is not available",
 *   "path":      "/api/holds/abc/confirm",
 *   "timestamp": "2026-02-25T12:00:00Z"
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(SeatHoldNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(
      SeatHoldNotFoundException ex, HttpServletRequest req) {
    return error(HttpStatus.NOT_FOUND, "HOLD_NOT_FOUND", ex.getMessage(), req);
  }

  @ExceptionHandler(SeatHoldConflictException.class)
  public ResponseEntity<Map<String, Object>> handleConflict(
      SeatHoldConflictException ex, HttpServletRequest req) {
    return error(HttpStatus.CONFLICT, "HOLD_CONFLICT", ex.getMessage(), req);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequest(
      IllegalArgumentException ex, HttpServletRequest req) {
    return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req);
  }

  private ResponseEntity<Map<String, Object>> error(
      HttpStatus status, String code, String message, HttpServletRequest req) {
    return ResponseEntity.status(status).body(Map.of(
        "status", status.value(),
        "code", code,
        "message", message != null ? message : "",
        "path", req.getRequestURI(),
        "timestamp", Instant.now().toString()));
  /**
   * Handles @Valid failures on request bodies.
   * Reports the first field error so clients know exactly what to fix.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    String firstError = ex.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
        .orElse("Validation failed");
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", firstError, req);
  }

  // -------------------------------------------------------------------------

  private ResponseEntity<Map<String, Object>> error(
      HttpStatus status, String code, String message, HttpServletRequest req) {
    return ResponseEntity.status(status).body(Map.of(
        "status",    status.value(),
        "code",      code,
        "message",   message != null ? message : "",
        "path",      req.getRequestURI(),
        "timestamp", Instant.now().toString()
    ));
  }
}
