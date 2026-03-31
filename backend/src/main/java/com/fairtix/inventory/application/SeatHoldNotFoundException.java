package com.fairtix.inventory.application;

/**
 * Thrown when a hold is not found for the given id + ownerId combination.
 * Mapped to HTTP 404 by {@link com.fairtix.config.GlobalExceptionHandler}.
 */
public class SeatHoldNotFoundException extends RuntimeException {

  public SeatHoldNotFoundException(String message) {
    super(message);
  }
}
