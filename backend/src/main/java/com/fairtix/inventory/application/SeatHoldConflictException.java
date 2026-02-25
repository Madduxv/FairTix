package com.fairtix.inventory.application;

/**
 * Thrown when a hold operation conflicts with the current seat or hold state
 * (e.g. seat unavailable, hold already released/expired, limit reached).
 * Mapped to HTTP 409 by {@link com.fairtix.config.GlobalExceptionHandler}.
 */
public class SeatHoldConflictException extends RuntimeException {

  public SeatHoldConflictException(String message) {
    super(message);
  }
}
