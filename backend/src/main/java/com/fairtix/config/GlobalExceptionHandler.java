package com.fairtix.config;

import com.fairtix.common.ResourceNotFoundException;
import com.fairtix.inventory.application.DuplicateSeatException;
import com.fairtix.inventory.application.SeatHoldConflictException;
import com.fairtix.inventory.application.SeatHoldNotFoundException;
import com.fairtix.orders.application.OrderNotFoundException;
import com.fairtix.auth.application.AccountLockedException;
import com.fairtix.auth.application.WeakPasswordException;
import com.fairtix.payments.api.PaymentProcessingException;
import com.fairtix.payments.application.PaymentFailedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

/**
 * Translates domain and validation exceptions into a consistent JSON error
 * body:
 * 
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

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

  @ExceptionHandler(DuplicateSeatException.class)
  public ResponseEntity<Map<String, Object>> handleDuplicateSeat(
      DuplicateSeatException ex, HttpServletRequest req) {
    return error(HttpStatus.CONFLICT, "DUPLICATE_SEAT", ex.getMessage(), req);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequest(
      IllegalArgumentException ex, HttpServletRequest req) {
    return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req);
  }

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

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> handleResponseStatus(
      ResponseStatusException ex, HttpServletRequest req) {
    HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
    if (status == null) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    String message = ex.getReason();
    if (message == null || message.isBlank()) {
      message = ex.getMessage();
      if (message == null || message.isBlank()) {
        message = status.getReasonPhrase();
      }
    }
    return error(status, status.name(), message, req);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest req) {
    return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied", req);
  }

  @ExceptionHandler(OrderNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleOrderNotFound(
      OrderNotFoundException ex, HttpServletRequest req) {
    return error(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", ex.getMessage(), req);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleResourceNotFound(
      ResourceNotFoundException ex, HttpServletRequest req) {
    return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), req);
  }

  @ExceptionHandler(AccountLockedException.class)
  public ResponseEntity<Map<String, Object>> handleAccountLocked(
      AccountLockedException ex, HttpServletRequest req) {
    Map<String, Object> body = Map.of(
        "status", HttpStatus.TOO_MANY_REQUESTS.value(),
        "code", "ACCOUNT_LOCKED",
        "message", ex.getMessage(),
        "remainingSeconds", ex.getRemainingSeconds(),
        "path", req.getRequestURI(),
        "timestamp", Instant.now().toString());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
  }

  @ExceptionHandler(WeakPasswordException.class)
  public ResponseEntity<Map<String, Object>> handleWeakPassword(
      WeakPasswordException ex, HttpServletRequest req) {
    return error(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", ex.getMessage(), req);
  }

  @ExceptionHandler(PaymentProcessingException.class)
  public ResponseEntity<Object> handlePaymentProcessing(
      PaymentProcessingException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(ex.getPaymentResponse());
  }

  @ExceptionHandler(PaymentFailedException.class)
  public ResponseEntity<Map<String, Object>> handlePaymentFailed(
      PaymentFailedException ex, HttpServletRequest req) {
    return error(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_FAILED", ex.getMessage(), req);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleDataIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest req) {
    log.error("Data integrity violation on {}: {}", req.getRequestURI(), ex.getMessage());
    return error(HttpStatus.CONFLICT, "DATA_CONFLICT",
        "The request conflicts with the current state of the data", req);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleUnexpected(
      Exception ex, HttpServletRequest req) {
    log.error("Unhandled exception on {}", req.getRequestURI(), ex);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
        "An unexpected error occurred", req);
  }

  // -------------------------------------------------------------------------

  private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message,
      HttpServletRequest req) {
    return ResponseEntity.status(status).body(Map.of(
        "status", status.value(),
        "code", code,
        "message", message != null ? message : "",
        "path", req.getRequestURI(),
        "timestamp", Instant.now().toString()));
  }
}
